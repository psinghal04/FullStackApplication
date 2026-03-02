package com.example.hrapp.common.api;

import java.time.OffsetDateTime;

/**
 * Canonical error response payload returned by REST endpoints.
 */
public record ApiErrorResponse(
    int status,
    String message,
    OffsetDateTime timestamp
) {
}
