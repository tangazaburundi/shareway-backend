package com.shareway.infrastructure.security.ratelimit;

import com.shareway.domain.model.SystemSetting;
import com.shareway.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Store dynamique piloté par l'admin via le réglage système {@code rateLimiterStore} :
 * <ul>
 *   <li>{@code redis}     → utilise Redis S'IL est joignable, sinon bascule en mémoire (log + avertissement).</li>
 *   <li>{@code in-memory} (défaut / absent) → mémoire locale.</li>
 * </ul>
 * La décision est recalculée périodiquement (par défaut toutes les 30 s), ce qui
 * permet de basculer à chaud :
 * <ul>
 *   <li>sans redémarrage si l'admin active Redis après installation ;</li>
 *   <li>avec repli automatique en mémoire si Redis tombe.</li>
 * </ul>
 * Aucun changement de comportement si Redis n'est pas installé : le dev local
 * (et la prod sans Redis) continuent en mémoire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicRateLimitStore implements RateLimitStore {

    public static final String MODE_KEY = "rateLimiterStore";
    public static final String MODE_REDIS = "redis";
    public static final String MODE_MEMORY = "in-memory";

    private final SystemSettingRepository settingRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${shareway.app.rate-limiter.refresh-seconds:30}")
    private long refreshSeconds;

    private static final InMemoryRateLimitStore IN_MEMORY = new InMemoryRateLimitStore();

    private volatile RedisRateLimitStore redisStore;
    private volatile boolean redisMode = false;
    private volatile long lastRefresh = 0L;

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        return resolveStore().tryAcquire(key, limit, window);
    }

    private RateLimitStore resolveStore() {
        refreshIfStale();
        if (redisMode) {
            if (redisStore == null) {
                redisStore = new RedisRateLimitStore(redisTemplate);
            }
            return redisStore;
        }
        return IN_MEMORY;
    }

    private void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh > refreshSeconds * 1000L) {
            synchronized (this) {
                // Double-check : un autre thread a peut-être rafraîchi entre-temps.
                if (now - lastRefresh > refreshSeconds * 1000L) {
                    refreshMode();
                    lastRefresh = System.currentTimeMillis();
                }
            }
        }
    }

    private void refreshMode() {
        String adminMode = readAdminSetting();

        if (MODE_REDIS.equalsIgnoreCase(adminMode)) {
            if (redisAvailable()) {
                if (!redisMode) {
                    log.info("Rate limiter basculé en mode Redis (le compteur est désormais partagé entre instances)");
                    redisMode = true;
                }
            } else {
                if (redisMode) {
                    log.warn("Rate limiter : Redis indisponible — bascule automatique en mémoire");
                    redisMode = false;
                } else {
                    log.warn("Rate limiter : réglage admin « redis » mais Redis injoignable — utilisation de la mémoire");
                }
            }
        } else {
            if (redisMode) {
                log.info("Rate limiter repassé en mémoire (réglage admin = {})", sanitize(adminMode));
                redisMode = false;
            } else if (adminMode != null && !MODE_MEMORY.equalsIgnoreCase(adminMode)) {
                log.warn("Rate limiter : valeur admin inconnue « {} » — utilisation de la mémoire", sanitize(adminMode));
            }
        }
    }

    private String readAdminSetting() {
        try {
            return settingRepository.findByKey(MODE_KEY)
                    .map(SystemSetting::getValue)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Rate limiter : lecture du réglage admin impossible ({}) — mémoire par défaut", e.getMessage());
            return null;
        }
    }

    private boolean redisAvailable() {
        try {
            Boolean ok = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                try {
                    return "PONG".equalsIgnoreCase(connection.ping());
                } catch (Exception e) {
                    return false;
                }
            });
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            return false;
        }
    }

    private String sanitize(String value) {
        return value == null ? "null" : value.replace("\n", "").replace("\r", "").substring(0, Math.min(value.length(), 50));
    }
}