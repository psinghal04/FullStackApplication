package com.example.hrapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested domain resource cannot be found.
 */
public final class ResourceNotFoundException extends HrAppException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
