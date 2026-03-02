package com.example.hrapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when request input violates business validation rules.
 */
public final class BadRequestException extends HrAppException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
