package com.quickbite.restaurant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Manages JWT token blacklisting using Redis.
 * When a user logs out, their access token is added to Redis with a TTL
 * matching the token's remaining lifetime. This ensures the token cannot
 * be reused after logout while automatically cleaning up expired entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Blacklists a JWT token for the given duration.
     *
     * @param token           the JWT access token to blacklist
     * @param expirationMillis remaining time (in ms) before the token naturally expires
     */
    public void blacklistToken(String token, long expirationMillis) {
        if (expirationMillis <= 0) {
            log.debug("Token already expired, skipping blacklist");
            return;
        }
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationMillis, TimeUnit.MILLISECONDS);
        log.info("JWT token blacklisted — TTL={}ms", expirationMillis);
    }

    /**
     * Checks if a JWT token has been blacklisted.
     *
     * @param token the JWT access token to check
     * @return true if the token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
