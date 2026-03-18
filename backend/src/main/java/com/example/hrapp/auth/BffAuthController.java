package com.example.hrapp.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Optional;

/**
 * BFF authentication controller.
 * Provides endpoints for session-based authentication.
 */
@RestController
@RequestMapping("/api/auth")
public class BffAuthController {
    private static final Logger log = LoggerFactory.getLogger(BffAuthController.class);

    private final BffSessionService sessionService;
    private final RequestOriginResolver requestOriginResolver;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Internal (Docker service name) Keycloak base URL for server-to-server calls. */
    @Value("${app.keycloak.internal-issuer-uri:${KEYCLOAK_ISSUER_URI:http://keycloak:8080/realms/hr}}")
    private String keycloakInternalIssuerUri;

    @Value("${oauth2.client-id:hr-frontend}")
    private String oauthClientId;

    public BffAuthController(BffSessionService sessionService, RequestOriginResolver requestOriginResolver) {
        this.sessionService = sessionService;
        this.requestOriginResolver = requestOriginResolver;
    }

    /**
     * Returns CSRF token for initial page load.
     * Accessible without authentication.
     *
     * <p>Spring Security 6 stores the CSRF token as a {@code Supplier<CsrfToken>} (deferred),
     * not as a {@code CsrfToken} directly. Calling {@code supplier.get()} materialises the token
     * and causes Spring Security to write the {@code XSRF-TOKEN} cookie on the response.</p>
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        // Spring Security 6: attribute is a Supplier<CsrfToken>, not a CsrfToken directly.
        Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
        CsrfToken csrfToken = null;
        if (csrfAttr instanceof java.util.function.Supplier<?> supplier) {
            Object resolved = supplier.get();
            if (resolved instanceof CsrfToken token) {
                csrfToken = token;
            }
        } else if (csrfAttr instanceof CsrfToken token) {
            // Fallback: older Spring Security versions store the token directly
            csrfToken = token;
        }

        if (csrfToken == null) {
            // Second fallback: CsrfTokenRequestAttributeHandler may have stored it at "_csrf"
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }

        if (csrfToken != null) {
            return ResponseEntity.ok(Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName()
            ));
        }

        return ResponseEntity.ok(Map.of());
    }

    /**
     * Returns current authenticated user information from BFF session.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
        @AuthenticationPrincipal BffSessionPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return ResponseEntity.ok(Map.of(
            "username", principal.username(),
            "email", principal.email() != null ? principal.email() : "",
            "employeeId", principal.employeeId() != null ? principal.employeeId() : "",
            "roles", principal.roles()
        ));
    }

    /**
     * Logout endpoint.
     *
     * <ol>
     *   <li>Revoke the BFF session from Redis.</li>
     *   <li>Call Keycloak's token-revocation endpoint server-side (backchannel) using the
     *       stored refresh token. This terminates the Keycloak SSO session without any
     *       browser round-trip, so no intermediate "you are logged out" page is shown.</li>
     *   <li>Return {@code {"logoutUrl": "/login"}} so the frontend redirects the browser
     *       directly to the app's own login page.</li>
     * </ol>
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthConstants.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    String sessionId = cookie.getValue();

                    // Load session BEFORE revoking so we can use its refresh token
                    Optional<BffSession> sessionOpt = sessionService.findSessionById(sessionId);
                    sessionService.revokeSession(sessionId);

                    // Clear BFF_SESSION_ID cookie in the browser
                    Cookie clearCookie = new Cookie(AuthConstants.SESSION_COOKIE_NAME, null);
                    clearCookie.setHttpOnly(true);
                    clearCookie.setSecure(false);
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    response.addCookie(clearCookie);

                    // Backchannel logout: revoke the Keycloak SSO session using the refresh token.
                    // This avoids the Keycloak front-channel confirmation page entirely.
                    sessionOpt.map(BffSession::refreshToken)
                              .ifPresent(this::revokeKeycloakSession);
                    break;
                }
            }
        }

        // Always send the browser to the app's own /login page.
        // The Keycloak SSO session is already gone (revoked above), so the user
        // will see the Keycloak login form when they authenticate next time.
        return ResponseEntity.ok(Map.of("logoutUrl", "/login"));
    }

    /**
     * Calls Keycloak's token endpoint to revoke the session tied to the given refresh token.
     * This is the backchannel (server-to-server) equivalent of RP-Initiated Logout — it
     * terminates the Keycloak SSO session without requiring a browser redirect to Keycloak.
     */
    private void revokeKeycloakSession(String refreshToken) {
        // Use internal (Docker service-name) URL for server-to-server calls — the public 
        // issuer URI uses localhost:8080 which is unreachable from inside the container.
        String logoutEndpoint = keycloakInternalIssuerUri + "/protocol/openid-connect/logout";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", oauthClientId);
            body.add("refresh_token", refreshToken);

            restTemplate.postForEntity(
                logoutEndpoint,
                new HttpEntity<>(body, headers),
                Void.class
            );
            log.debug("Keycloak session revoked via backchannel logout");
        } catch (Exception e) {
            // Non-fatal: BFF session is already cleared. Log and continue.
            log.warn("Backchannel Keycloak logout failed (session may already be expired): {}", e.getMessage());
        }
    }

    /**
     * Initiates OAuth2 login flow by redirecting to OAuth2 authorization endpoint.
     * Spring Security will handle the redirect to Keycloak.
     */
    @GetMapping("/login")
    public void login(
        @RequestParam(name = "error", required = false) String error,
        HttpServletResponse response,
        HttpServletRequest request
    ) throws java.io.IOException {
        if (error != null) {
            response.sendRedirect("/login?authError=1");
            return;
        }

        String redirectUrl = requestOriginResolver.resolveBaseUrl(request) + AuthConstants.LOGIN_REDIRECT_PATH;
        response.sendRedirect(redirectUrl);
    }
}
