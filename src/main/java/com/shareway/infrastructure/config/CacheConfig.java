package com.shareway.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public SimpleCacheManager cacheManager() {
        var manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("trips", 300, 500),
                buildCache("users", 600, 200),
                buildCache("dashboard_stats", 60, 10),
                buildCache("notifications", 30, 1000)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int ttlSeconds, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
