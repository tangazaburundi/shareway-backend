package com.shareway.infrastructure.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Store partagé Redis — compteurs globaux entre toutes les instances.
 * <p>
 * Implémentation simple et fiable : compteur fixe par fenêtre (INCR + EXPIRE).
 * En cas d'indisponibilité de Redis en cours de route, on laisse passer la
 * requête (fail-open) : le {@link DynamicRateLimitStore} rebascule alors en
 * mémoire au prochain refresh.
 */
@Slf4j
public class RedisRateLimitStore implements RateLimitStore {

    private static final String KEY_PREFIX = "shareway:ratelimit:";

    private final StringRedisTemplate redis;

    public RedisRateLimitStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        try {
            String redisKey = KEY_PREFIX + key;
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, window);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            log.warn("Rate limiter: Redis indisponible, requête autorisée par défaut : {}", e.getMessage());
            return true;
        }
    }
}