package com.example.hrapp.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String employeeRole;
    private String employeeDomain;
    private String temporaryPassword;
    private boolean temporaryPasswordEnabled;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(String employeeRole) {
        this.employeeRole = employeeRole;
    }

    public String getEmployeeDomain() {
        return employeeDomain;
    }

    public void setEmployeeDomain(String employeeDomain) {
        this.employeeDomain = employeeDomain;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public boolean isTemporaryPasswordEnabled() {
        return temporaryPasswordEnabled;
    }

    public void setTemporaryPasswordEnabled(boolean temporaryPasswordEnabled) {
        this.temporaryPasswordEnabled = temporaryPasswordEnabled;
    }
}
