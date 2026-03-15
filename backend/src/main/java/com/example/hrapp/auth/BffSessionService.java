package com.example.hrapp.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing BFF sessions.
 */
@Service
public class BffSessionService {
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);

    private final BffSessionRepository sessionRepository;

    public BffSessionService(BffSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Create a new BFF session.
     */
    public BffSession createSession(
        String employeeId,
        String username,
        String email,
        List<String> roles,
        String accessToken,
        String idToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
    ) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        BffSession session = new BffSession(
            sessionId,
            employeeId,
            username,
            email,
            roles,
            accessToken,
            idToken,
            refreshToken,
            accessTokenExpiresAt,
            refreshTokenExpiresAt,
            now,
            now
        );

        sessionRepository.save(session);
        return session;
    }

    /**
     * Validate and retrieve a session.
     * Returns empty if session doesn't exist or is expired.
     */
    public Optional<BffSession> validateSession(String sessionId) {
        Optional<BffSession> sessionOpt = sessionRepository.findById(sessionId);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        BffSession session = sessionOpt.get();
        
        // Check if refresh token is expired
        if (session.isRefreshTokenExpired()) {
            sessionRepository.deleteById(sessionId);
            return Optional.empty();
        }

        // Update last accessed time and refresh TTL
        BffSession updatedSession = new BffSession(
            session.sessionId(),
            session.employeeId(),
            session.username(),
            session.email(),
            session.roles(),
            session.accessToken(),
            session.idToken(),
            session.refreshToken(),
            session.accessTokenExpiresAt(),
            session.refreshTokenExpiresAt(),
            session.createdAt(),
            Instant.now()
        );

        sessionRepository.save(updatedSession);
        return Optional.of(updatedSession);
    }

    /**
     * Delete a session.
     */
    public void revokeSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    /**
     * Find a session by ID without updating last-accessed time.
     * Used to read session data (e.g. id_token) before revoking.
     */
    public Optional<BffSession> findSessionById(String sessionId) {
        return sessionRepository.findById(sessionId);
    }
}
