package com.example.hrapp.security;

import com.example.hrapp.identity.KeycloakAdminProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central Spring Security configuration for the resource server.
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
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
        TerminatedEmployeeFilter terminatedEmployeeFilter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            .addFilterAfter(terminatedEmployeeFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
