package com.example.hrapp.identity;

import com.example.hrapp.common.exception.KeycloakProvisioningException;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.function.Predicate;

/**
 * Adapter over Keycloak Admin REST APIs for employee identity lifecycle operations.
 *
 * <p>The client intentionally centralizes IAM side effects (upsert, role normalization,
 * temporary password setup, enabled/disabled synchronization) so service-layer code remains
 * focused on domain workflows.</p>
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
            .baseUrl(properties.serverUrl())
            .build();
    }

    /**
     * Upserts an employee user and normalizes role mappings to employee-only role.
     */
    @Retry(name = "keycloakAdminApi", fallbackMethod = "upsertEmployeeUserFallback")
    public void upsertEmployeeUser(String employeeId, String emailAddress, String firstName, String lastName) {
        upsertEmployeeUserInternal(employeeId, emailAddress, firstName, lastName);
    }

    /**
     * Synchronizes Keycloak account enabled state by user email.
     */
    @Retry(name = "keycloakAdminApi", fallbackMethod = "setUserEnabledByEmailFallback")
    public void setUserEnabledByEmail(String emailAddress, boolean enabled) {
        Optional.ofNullable(emailAddress)
            .map(this::normalizeLower)
            .filter(Predicate.not(String::isBlank))
            .ifPresent(normalizedEmail -> {
                String accessToken = fetchAdminAccessToken();
                String username = buildUsername(normalizedEmail);

                findUserId(accessToken, username)
                    .ifPresentOrElse(
                        userId -> updateUserEnabledState(accessToken, userId, enabled),
                        () -> log.info("No Keycloak user found for email={}, skipping enabled sync", normalizedEmail)
                    );
            });
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
        syncRealmRolesToEmployeeOnly(accessToken, userId, properties.employeeRole());
    }

    private String fetchAdminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        Map<String, Object> response = restClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("access_token") == null) {
            throw new RestClientException("Missing access token from Keycloak");
        }
        return response.get("access_token").toString();
    }

    private Optional<String> findUserId(String accessToken, String username) {
        List<Map<String, Object>> users = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/admin/realms/{realm}/users")
                .queryParam("username", username)
                .queryParam("exact", true)
                .build(properties.realm()))
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return Optional.ofNullable(users)
            .filter(Predicate.not(List::isEmpty))
            .map(result -> result.get(0))
            .map(result -> result.get("id"))
            .map(Object::toString);
    }

    private String createUser(String accessToken, String username, String emailAddress, String employeeId, String firstName, String lastName) {
        restClient.post()
            .uri("/admin/realms/{realm}/users", properties.realm())
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
            .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildUserPayload(username, emailAddress, employeeId, firstName, lastName, true))
            .retrieve()
            .toBodilessEntity();
    }

    private void setTemporaryPassword(String accessToken, String userId) {
        String temporaryPassword = properties.temporaryPassword();
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            return;
        }

        Map<String, Object> passwordPayload = Map.of(
            "type", "password",
            "value", temporaryPassword,
            "temporary", properties.temporaryPasswordEnabled()
        );

        restClient.put()
            .uri("/admin/realms/{realm}/users/{userId}/reset-password", properties.realm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(passwordPayload)
            .retrieve()
            .toBodilessEntity();
    }

    private void syncRealmRolesToEmployeeOnly(String accessToken, String userId, String roleName) {
        List<Map<String, Object>> userRoles = restClient.get()
            .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.realm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        if (userRoles != null && !userRoles.isEmpty()) {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(userRoles)
                .retrieve()
                .toBodilessEntity();
        }

        Map<String, Object> roleRepresentation = restClient.get()
            .uri("/admin/realms/{realm}/roles/{roleName}", properties.realm(), roleName)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        restClient.post()
            .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.realm(), userId)
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
        return normalizeLower(emailAddress);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private void updateUserEnabledState(String accessToken, String userId, boolean enabled) {
        Map<String, Object> currentUser = restClient.get()
            .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        Map<String, Object> payload = currentUser == null ? new HashMap<>() : new HashMap<>(currentUser);
        payload.put("enabled", enabled);

        restClient.put()
            .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), userId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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
