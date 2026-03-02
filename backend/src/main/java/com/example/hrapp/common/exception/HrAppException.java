package com.example.hrapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Sealed base type for domain/application exceptions exposed through the REST API.
 */
public abstract sealed class HrAppException extends RuntimeException
    permits BadRequestException, AccessDeniedException, ResourceNotFoundException, KeycloakProvisioningException {

    private final HttpStatus httpStatus;

    protected HrAppException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    protected HrAppException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}