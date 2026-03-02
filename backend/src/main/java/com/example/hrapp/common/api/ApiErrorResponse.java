package com.example.hrapp.common.api;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    int status,
    String message,
    OffsetDateTime timestamp
) {
}
