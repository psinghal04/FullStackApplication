package com.example.hrapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Custom OAuth2 Client Configuration to avoid issuer validation issues
 * in Docker networking where Keycloak advertises localhost but backend
 * connects via service name.
 */
@Configuration
public class OAuth2ClientConfig {

    @Value("${oauth2.client-id:hr-frontend}")
    private String clientId;

    @Value("${oauth2.client-secret:}")
    private String clientSecret;

    @Value("${oauth2.redirect-uri:http://localhost:4200/login/oauth2/code/keycloak}")
    private String redirectUri;

    @Value("${oauth2.authorization-uri:http://localhost:8080/realms/hr/protocol/openid-connect/auth}")
    private String authorizationUri;

    @Value("${oauth2.token-uri:http://keycloak:8080/realms/hr/protocol/openid-connect/token}")
    private String tokenUri;

    @Value("${oauth2.user-info-uri:http://keycloak:8080/realms/hr/protocol/openid-connect/userinfo}")
    private String userInfoUri;

    @Value("${oauth2.jwk-set-uri:http://keycloak:8080/realms/hr/protocol/openid-connect/certs}")
    private String jwkSetUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(this.keycloakClientRegistration());
    }

    private ClientRegistration keycloakClientRegistration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .userInfoUri(userInfoUri)
                .jwkSetUri(jwkSetUri)
                .userNameAttributeName("preferred_username")
                .clientName("Keycloak")
                .build();
    }
}
