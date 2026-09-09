package com.shareway.infrastructure.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Store en mémoire locale (par instance JVM).
 * <p>
 * Utilise Bucket4j (fenêtre glissante à recharge progressive) et un cache
 * Caffeine à taille limitée pour éviter toute fuite mémoire liée aux clés.
 * Les compteurs sont perdus au redémarrage et non partagés entre instances :
 * c'est le fallback par défaut quand Redis n'est pas activé/disponible.
 */
public class InMemoryRateLimitStore implements RateLimitStore {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(limit, Refill.greedy(limit, window)))
                .build());
        return bucket != null && bucket.tryConsume(1);
    }
}