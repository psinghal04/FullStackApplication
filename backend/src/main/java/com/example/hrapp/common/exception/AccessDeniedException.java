package com.example.hrapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the caller is authenticated but not authorized for an operation.
 */
public final class AccessDeniedException extends HrAppException {

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
