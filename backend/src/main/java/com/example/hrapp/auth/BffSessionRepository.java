package com.example.hrapp.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed repository for BFF sessions.
 */
@Repository
public class BffSessionRepository {
    private static final String SESSION_PREFIX = "bff:session:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BffSessionRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(BffSession session) {
        try {
            String key = SESSION_PREFIX + session.sessionId();
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize session", e);
        }
    }

    public Optional<BffSession> findById(String sessionId) {
        try {
            String key = SESSION_PREFIX + sessionId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            BffSession session = objectMapper.readValue(json, BffSession.class);
            return Optional.of(session);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize session", e);
        }
    }

    public void deleteById(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    public void updateTtl(String sessionId, Duration ttl) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.expire(key, ttl);
    }
}
