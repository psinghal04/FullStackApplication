package com.example.hrapp.auth;

import com.example.hrapp.employee.EmployeeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Creates a BFF session after successful OAuth2 login.
 * Sets HttpOnly session cookie.
 */
@Component
public class BffOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final String SESSION_COOKIE_NAME = "BFF_SESSION_ID";
    private static final int COOKIE_MAX_AGE = 30 * 60; // 30 minutes

    private final BffSessionService sessionService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ObjectMapper objectMapper;
    private final EmployeeRepository employeeRepository;

    public BffOAuth2LoginSuccessHandler(
        BffSessionService sessionService,
        OAuth2AuthorizedClientService authorizedClientService,
        ObjectMapper objectMapper,
        EmployeeRepository employeeRepository
    ) {
        this.sessionService = sessionService;
        this.authorizedClientService = authorizedClientService;
        this.objectMapper = objectMapper;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            response.sendRedirect("/");
            return;
        }

        OAuth2User oauth2User = oauth2Token.getPrincipal();
        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

        // Get authorized client to access tokens
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
            registrationId,
            oauth2User.getName()
        );

        if (authorizedClient == null) {
            response.sendRedirect("/");
            return;
        }

        // Extract user information
        String username = oauth2User.getName();
        String email = extractEmail(oauth2User);
        String employeeId = extractEmployeeId(oauth2User);
        // Extract roles from the access token JWT (userinfo endpoint does not include realm_access)
        List<String> roles = extractRolesFromJwt(authorizedClient.getAccessToken().getTokenValue());

        // Get tokens
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String idToken = (oauth2User instanceof OidcUser oidcUser && oidcUser.getIdToken() != null)
            ? oidcUser.getIdToken().getTokenValue() : null;
        String refreshToken = authorizedClient.getRefreshToken() != null 
            ? authorizedClient.getRefreshToken().getTokenValue() 
            : null;
        Instant accessTokenExpiresAt = authorizedClient.getAccessToken().getExpiresAt();
        Instant refreshTokenExpiresAt = authorizedClient.getRefreshToken() != null
            ? authorizedClient.getRefreshToken().getExpiresAt()
            : null;

        // Create BFF session
        BffSession session = sessionService.createSession(
            employeeId,
            username,
            email,
            roles,
            accessToken,
            idToken,
            refreshToken,
            accessTokenExpiresAt,
            refreshTokenExpiresAt
        );

        // Set HttpOnly session cookie with SameSite=Lax.
        // Use addHeader (not setHeader) so we do NOT replace any JSESSIONID cookie that
        // Spring Security's session fixation protection already wrote to the response.
        String cookieValue = String.format("%s=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
            SESSION_COOKIE_NAME, session.sessionId(), COOKIE_MAX_AGE);
        response.addHeader("Set-Cookie", cookieValue);

        // Redirect to frontend home page
        String scheme = request.getHeader("X-Forwarded-Proto") != null 
            ? request.getHeader("X-Forwarded-Proto") 
            : request.getScheme();
        String host = request.getHeader("X-Forwarded-Host") != null 
            ? request.getHeader("X-Forwarded-Host") 
            : request.getHeader("Host");
        
        // Redirect to employee profile page to avoid additional redirect from root
        String redirectUrl = scheme + "://" + host + "/employee/profile";
        response.sendRedirect(redirectUrl);
    }

    private String extractEmail(OAuth2User oauth2User) {
        if (oauth2User instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return oauth2User.getAttribute("email");
    }

    private String extractEmployeeId(OAuth2User oauth2User) {
        // Try to get employee_id claim from userinfo attributes
        Object employeeId = oauth2User.getAttribute("employee_id");
        if (employeeId != null) {
            return employeeId.toString();
        }
        // Fall back to DB lookup by email — covers EMPLOYEE users whose Keycloak
        // account does not have the employee_id custom attribute mapped.
        String email = extractEmail(oauth2User);
        if (email != null && !email.isBlank()) {
            return employeeRepository.findByEmailAddressIgnoreCase(email)
                    .map(e -> e.getEmployeeId())
                    .orElse(null);
        }
        return null;
    }

    /**
     * Extracts Keycloak realm roles from the raw JWT access token payload.
     * The userinfo endpoint does not include realm_access, but the access token does.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromJwt(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) return List.of();
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {});
            Object realmAccess = claims.get("realm_access");
            if (realmAccess instanceof Map<?, ?> ra && ra.get("roles") instanceof List<?> roleList) {
                return roleList.stream()
                        .filter(r -> r instanceof String)
                        .map(r -> (String) r)
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to extract roles from JWT: " + e.getMessage());
        }
        return List.of();
    }
}
