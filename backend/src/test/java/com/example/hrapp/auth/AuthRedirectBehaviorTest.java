package com.example.hrapp.auth;

import com.example.hrapp.employee.EmployeeRepository;
import com.example.hrapp.security.JwtClaimNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRedirectBehaviorTest {

    private final RequestOriginResolver requestOriginResolver = new RequestOriginResolver();

    @Test
    void requestOriginResolver_prefersForwardedHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setScheme("http");
        request.addHeader("Host", "internal:8081");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "app.example.com");

        assertThat(requestOriginResolver.resolveBaseUrl(request)).isEqualTo("https://app.example.com");
    }

    @Test
    void requestOriginResolver_fallsBackToRequestSchemeAndHost() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setScheme("https");
        request.addHeader("Host", "localhost:4200");

        assertThat(requestOriginResolver.resolveBaseUrl(request)).isEqualTo("https://localhost:4200");
    }

    @Test
    void loginRedirect_usesResolvedOrigin() throws Exception {
        BffSessionService sessionService = mock(BffSessionService.class);
        BffAuthController controller = new BffAuthController(sessionService, requestOriginResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setScheme("http");
        request.addHeader("Host", "backend:8080");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "hr.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(null, response, request);

        assertThat(response.getRedirectedUrl()).isEqualTo("https://hr.example.com" + AuthConstants.LOGIN_REDIRECT_PATH);
    }

    @Test
    void oauthLoginSuccessRedirect_usesResolvedOrigin() throws Exception {
        BffSessionService sessionService = mock(BffSessionService.class);
        OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        BffOAuth2LoginSuccessHandler handler = new BffOAuth2LoginSuccessHandler(
            sessionService,
            authorizedClientService,
            new ObjectMapper(),
            employeeRepository,
            requestOriginResolver
        );

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("keycloak")
            .clientId("client-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri("https://issuer.example.com/auth")
            .tokenUri("https://issuer.example.com/token")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .userInfoUri("https://issuer.example.com/userinfo")
            .userNameAttributeName("sub")
            .build();

        DefaultOAuth2User principal = new DefaultOAuth2User(
            AuthorityUtils.NO_AUTHORITIES,
            Map.of("sub", "user-1", JwtClaimNames.EMAIL, "jane@example.com", JwtClaimNames.EMPLOYEE_ID, "EMP-000123"),
            "sub"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
            principal,
            principal.getAuthorities(),
            "keycloak"
        );

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "header.eyJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiRU1QTE9ZRUUiXX19.signature",
            Instant.parse("2026-03-18T12:00:00Z"),
            Instant.parse("2026-03-18T13:00:00Z")
        );
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, principal.getName(), accessToken);
        when(authorizedClientService.loadAuthorizedClient("keycloak", principal.getName())).thenReturn(authorizedClient);
        when(sessionService.createSession(
            "EMP-000123",
            "user-1",
            "jane@example.com",
            java.util.List.of("EMPLOYEE"),
            accessToken.getTokenValue(),
            null,
            null,
            accessToken.getExpiresAt(),
            null
        )).thenReturn(new BffSession(
            "session-1",
            "EMP-000123",
            "user-1",
            "jane@example.com",
            java.util.List.of("EMPLOYEE"),
            accessToken.getTokenValue(),
            null,
            null,
            accessToken.getExpiresAt(),
            null,
            Instant.parse("2026-03-18T12:00:00Z"),
            Instant.parse("2026-03-18T12:00:00Z")
        ));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/keycloak");
        request.setScheme("http");
        request.addHeader("Host", "backend:8080");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "hr.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("https://hr.example.com" + AuthConstants.EMPLOYEE_PROFILE_PATH);
        assertThat(response.getHeader("Set-Cookie")).contains(AuthConstants.SESSION_COOKIE_NAME + "=session-1");
    }
}
