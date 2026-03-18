package com.example.hrapp.security;

import com.example.hrapp.identity.KeycloakAdminProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration supporting both:
 * - OAuth2 Resource Server (bearer token validation for API calls)
 * - OAuth2 Login (session-based BFF authentication)
 *
 * <p>The API relies on bearer JWT validation plus method-level authorization. In addition,
 * {@link TerminatedEmployeeFilter} is inserted after bearer authentication so it can enforce
 * employee lifecycle constraints using the resolved authenticated principal.</p>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class SecurityConfig {

    /**
     * Builds the HTTP security filter chain.
     * Supports both OAuth2 login (for BFF) and resource server (for API bearer tokens).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
        TerminatedEmployeeFilter terminatedEmployeeFilter,
        com.example.hrapp.auth.BffOAuth2LoginSuccessHandler loginSuccessHandler,
        com.example.hrapp.auth.BffSessionAuthenticationFilter sessionAuthenticationFilter,
        com.example.hrapp.auth.HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository
    ) throws Exception {
        // CSRF token handler for cookie-based tokens
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/api/v1/employees/**", "/api/v2/employees/**", "/api/auth/logout") // Employee API uses session cookies + BFF, not CSRF tokens; logout must always be reachable
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/auth/csrf", "/api/auth/me", "/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                .successHandler(loginSuccessHandler)
            )
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            .addFilterAfter(sessionAuthenticationFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(terminatedEmployeeFilter, com.example.hrapp.auth.BffSessionAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration allowing credentials (cookies) from frontend origin.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
