package com.example.hrapp.auth;

import java.time.Instant;
import java.util.List;

/**
 * BFF session data stored in Redis.
 * Contains user identity and OAuth2 tokens.
 */
public record BffSession(
    String sessionId,
    String employeeId,
    String username,
    String email,
    List<String> roles,
    String accessToken,
    String idToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    Instant createdAt,
    Instant lastAccessedAt
) {
    public boolean isAccessTokenExpired() {
        return accessTokenExpiresAt != null && Instant.now().isAfter(accessTokenExpiresAt);
    }

    public boolean isRefreshTokenExpired() {
        return refreshTokenExpiresAt != null && Instant.now().isAfter(refreshTokenExpiresAt);
    }
}
