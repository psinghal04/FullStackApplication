package com.example.hrapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when identity provisioning/synchronization with Keycloak fails after retries.
 */
public final class KeycloakProvisioningException extends HrAppException {

    public KeycloakProvisioningException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
