package com.xzy.forum.service;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.config.ForumRateLimitProperties;
import com.xzy.forum.exception.ApplicationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final ForumRateLimitProperties rateLimitProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    public RateLimitService(ForumRateLimitProperties rateLimitProperties,
                            ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.rateLimitProperties = rateLimitProperties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void check(ForumRateLimitProperties.LimitRule rule, String key, String message) {
        if (!rateLimitProperties.isEnabled() || rule == null || key == null || key.isBlank()) {
            return;
        }
        boolean allowed = tryConsume(key, rule.getMaxRequests(), Duration.ofSeconds(rule.getWindowSeconds()));
        if (!allowed) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_RATE_LIMITED.getCode(), message));
        }
    }

    private boolean tryConsume(String key, int maxRequests, Duration window) {
        if (maxRequests <= 0 || window.isZero() || window.isNegative()) {
            return true;
        }
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redisTemplate.expire(key, window);
                }
                return count == null || count <= maxRequests;
            } catch (Exception ignored) {
                // fallback to local limiter when redis is unavailable
            }
        }
        return tryConsumeLocally(key, maxRequests, window);
    }

    private boolean tryConsumeLocally(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        LocalCounter counter = localCounters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new LocalCounter(new AtomicInteger(0), now.plus(window));
            }
            return existing;
        });
        int current = counter.counter.incrementAndGet();
        if (counter.expiresAt.isBefore(now)) {
            localCounters.remove(key);
        }
        return current <= maxRequests;
    }

    private static class LocalCounter {
        private final AtomicInteger counter;
        private final Instant expiresAt;

        private LocalCounter(AtomicInteger counter, Instant expiresAt) {
            this.counter = counter;
            this.expiresAt = expiresAt;
        }
    }
}
