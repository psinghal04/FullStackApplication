package com.example.hrapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis cache configuration for application-level read caching.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures Redis cache manager with JSON value serialization and TTL.
     *
     * <p>Default typing is enabled on a copy of the application object mapper to preserve
     * polymorphic DTO types when values are deserialized from Redis.</p>
     */
    @Bean
    public RedisCacheManager redisCacheManager(
        RedisConnectionFactory redisConnectionFactory,
        @Value("${app.cache.employee-ttl-seconds:60}") long ttlSeconds
    ) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(ttlSeconds))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .build();
    }
}
