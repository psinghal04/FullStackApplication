package com.example.hrapp.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for Keycloak admin API access and employee provisioning defaults.
 */
@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
    String serverUrl,
    String realm,
    String clientId,
    String clientSecret,
    String employeeRole,
    String employeeDomain,
    String temporaryPassword,
    boolean temporaryPasswordEnabled
) {
}
