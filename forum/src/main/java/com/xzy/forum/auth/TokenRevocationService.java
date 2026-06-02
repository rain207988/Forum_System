package com.xzy.forum.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationService {

    private static final String TOKEN_BLACKLIST_PREFIX = "forum:auth:token:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Instant> localBlacklist = new ConcurrentHashMap<>();

    public TokenRevocationService(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void revoke(String token, Instant expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null) {
            return;
        }

        String tokenKey = buildTokenKey(token);
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(tokenKey, Boolean.TRUE, ttl);
                return;
            } catch (Exception ignored) {
                // fallback to local blacklist when redis is unavailable
            }
        }

        localBlacklist.put(tokenKey, expiresAt);
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String tokenKey = buildTokenKey(token);
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
            } catch (Exception ignored) {
                // fallback to local blacklist when redis is unavailable
            }
        }

        Instant expiresAt = localBlacklist.get(tokenKey);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            localBlacklist.remove(tokenKey);
            return false;
        }
        return true;
    }

    private String buildTokenKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + DigestUtils.sha256Hex(token);
    }
}
