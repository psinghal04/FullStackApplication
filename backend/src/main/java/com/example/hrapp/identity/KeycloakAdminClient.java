package com.example.hrapp.identity;

import com.example.hrapp.common.exception.KeycloakProvisioningException;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.getServerUrl())
            .build();
    }

    @Retry(name = "keycloakAdminApi", fallbackMethod = "upsertEmployeeUserFallback")
    public void upsertEmployeeUser(String employeeId, String emailAddress, String firstName, String lastName) {
        upsertEmployeeUserInternal(employeeId, emailAddress, firstName, lastName);
    }

    @Retry(name = "keycloakAdminApi", fallbackMethod = "setUserEnabledByEmailFallback")
    public void setUserEnabledByEmail(String emailAddress, boolean enabled) {
        String normalizedEmail = emailAddress == null ? "" : emailAddress.trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            return;
        }

        String accessToken = fetchAdminAccessToken();
        String username = buildUsername(normalizedEmail);

        Optional<String> userIdOpt = findUserId(accessToken, username);
        if (userIdOpt.isEmpty()) {
            log.info("No Keycloak user found for email={}, skipping enabled sync", normalizedEmail);
            return;
        }

        String userId = userIdOpt.get();
        Map<String, Object> currentUser = restClient.get()
            .uri("/admin/realms/{realm}/users/{userId}", properties.getRealm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(Map.class);

        Map<String, Object> payload = currentUser == null ? new HashMap<>() : new HashMap<>(currentUser);
        payload.put("enabled", enabled);

        restClient.put()
            .uri("/admin/realms/{realm}/users/{userId}", properties.getRealm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
    }

    private void upsertEmployeeUserInternal(String employeeId, String emailAddress, String firstName, String lastName) {
        String accessToken = fetchAdminAccessToken();
        String username = buildUsername(emailAddress);
        String normalizedFirstName = normalizeName(firstName);
        String normalizedLastName = normalizeName(lastName);

        String userId = findUserId(accessToken, username)
            .orElseGet(() -> createUser(accessToken, username, emailAddress, employeeId, normalizedFirstName, normalizedLastName));

        updateUser(accessToken, userId, username, emailAddress, employeeId, normalizedFirstName, normalizedLastName);
        setTemporaryPassword(accessToken, userId);
        syncRealmRolesToEmployeeOnly(accessToken, userId, properties.getEmployeeRole());
    }

    private String fetchAdminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        Map<String, Object> response = restClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new RestClientException("Missing access token from Keycloak");
        }
        return response.get("access_token").toString();
    }

    @SuppressWarnings("unchecked")
    private Optional<String> findUserId(String accessToken, String username) {
        List<Map<String, Object>> users = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/admin/realms/{realm}/users")
                .queryParam("username", username)
                .queryParam("exact", true)
                .build(properties.getRealm()))
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(List.class);

        if (users == null || users.isEmpty()) {
            return Optional.empty();
        }

        Object id = users.get(0).get("id");
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }

    private String createUser(String accessToken, String username, String emailAddress, String employeeId, String firstName, String lastName) {
        restClient.post()
            .uri("/admin/realms/{realm}/users", properties.getRealm())
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildUserPayload(username, emailAddress, employeeId, firstName, lastName, true))
            .retrieve()
            .toBodilessEntity();

        return findUserId(accessToken, username)
            .orElseThrow(() -> new RestClientException("Unable to resolve newly created Keycloak user"));
    }

    private void updateUser(String accessToken, String userId, String username, String emailAddress, String employeeId, String firstName, String lastName) {
        restClient.put()
            .uri("/admin/realms/{realm}/users/{userId}", properties.getRealm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildUserPayload(username, emailAddress, employeeId, firstName, lastName, true))
            .retrieve()
            .toBodilessEntity();
    }

    private void setTemporaryPassword(String accessToken, String userId) {
        if (properties.getTemporaryPassword() == null || properties.getTemporaryPassword().isBlank()) {
            return;
        }

        Map<String, Object> passwordPayload = Map.of(
            "type", "password",
            "value", properties.getTemporaryPassword(),
            "temporary", properties.isTemporaryPasswordEnabled()
        );

        restClient.put()
            .uri("/admin/realms/{realm}/users/{userId}/reset-password", properties.getRealm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(passwordPayload)
            .retrieve()
            .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private void syncRealmRolesToEmployeeOnly(String accessToken, String userId, String roleName) {
        List<Map<String, Object>> userRoles = restClient.get()
            .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.getRealm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(List.class);

        if (userRoles != null && !userRoles.isEmpty()) {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.getRealm(), userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(userRoles)
                .retrieve()
                .toBodilessEntity();
        }

        Map<String, Object> roleRepresentation = restClient.get()
            .uri("/admin/realms/{realm}/roles/{roleName}", properties.getRealm(), roleName)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(Map.class);

        restClient.post()
            .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.getRealm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(List.of(roleRepresentation))
            .retrieve()
            .toBodilessEntity();
    }

    private Map<String, Object> buildUserPayload(
        String username,
        String emailAddress,
        String employeeId,
        String firstName,
        String lastName,
        boolean enabled
    ) {
        return Map.of(
            "username", username,
            "enabled", enabled,
            "email", emailAddress,
            "firstName", firstName,
            "lastName", lastName,
            "attributes", Map.of("employee_id", List.of(employeeId))
        );
    }

    private String buildUsername(String emailAddress) {
        return emailAddress == null ? "" : emailAddress.trim().toLowerCase();
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    @SuppressWarnings("unused")
    private void upsertEmployeeUserFallback(
        String employeeId,
        String emailAddress,
        String firstName,
        String lastName,
        Exception exception
    ) {
        log.error(
            "Keycloak provisioning failed after retries for employeeId={} email={} firstName={} lastName={}",
            employeeId,
            emailAddress,
            firstName,
            lastName,
            exception
        );
        throw new KeycloakProvisioningException("Failed to provision employee in Keycloak after retries", exception);
    }

    @SuppressWarnings("unused")
    private void setUserEnabledByEmailFallback(String emailAddress, boolean enabled, Exception exception) {
        log.error(
            "Keycloak enabled sync failed after retries for email={} enabled={}",
            emailAddress,
            enabled,
            exception
        );
        throw new KeycloakProvisioningException("Failed to synchronize employee enabled status in Keycloak", exception);
    }
}
