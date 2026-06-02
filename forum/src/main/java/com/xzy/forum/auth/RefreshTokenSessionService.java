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
public class RefreshTokenSessionService {

    private static final String REFRESH_SESSION_PREFIX = "forum:auth:refresh:session:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, RefreshSession> localSessions = new ConcurrentHashMap<>();

    public RefreshTokenSessionService(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void store(Long userId, String sessionId, String refreshToken, Instant expiresAt) {
        if (userId == null || sessionId == null || sessionId.isBlank() || refreshToken == null || refreshToken.isBlank() || expiresAt == null) {
            return;
        }

        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        String sessionKey = buildSessionKey(userId, sessionId);
        String tokenHash = hashToken(refreshToken);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(sessionKey, tokenHash, ttl);
                return;
            } catch (Exception ignored) {
                // fallback to local session store when redis is unavailable
            }
        }

        localSessions.put(sessionKey, new RefreshSession(tokenHash, expiresAt));
    }

    public boolean matches(Long userId, String sessionId, String refreshToken) {
        if (userId == null || sessionId == null || sessionId.isBlank() || refreshToken == null || refreshToken.isBlank()) {
            return false;
        }

        String sessionKey = buildSessionKey(userId, sessionId);
        String expectedHash = hashToken(refreshToken);
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(sessionKey);
                return expectedHash.equals(value);
            } catch (Exception ignored) {
                // fallback to local session store when redis is unavailable
            }
        }

        RefreshSession session = localSessions.get(sessionKey);
        if (session == null) {
            return false;
        }
        if (session.expiresAt.isBefore(Instant.now())) {
            localSessions.remove(sessionKey);
            return false;
        }
        return expectedHash.equals(session.tokenHash);
    }

    public void revoke(Long userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }

        String sessionKey = buildSessionKey(userId, sessionId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(sessionKey);
                return;
            } catch (Exception ignored) {
                // fallback to local session store when redis is unavailable
            }
        }

        localSessions.remove(sessionKey);
    }

    private String buildSessionKey(Long userId, String sessionId) {
        return REFRESH_SESSION_PREFIX + userId + ":" + sessionId;
    }

    private String hashToken(String refreshToken) {
        return DigestUtils.sha256Hex(refreshToken);
    }

    private record RefreshSession(String tokenHash, Instant expiresAt) {
    }
}
