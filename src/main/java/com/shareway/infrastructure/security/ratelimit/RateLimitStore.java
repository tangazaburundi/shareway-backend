package com.shareway.infrastructure.security.ratelimit;

import java.time.Duration;

/**
 * Store des compteurs de rate limiting.
 * <p>
 * Deux implémentations :
 * <ul>
 *   <li>{@link InMemoryRateLimitStore} : mémoire locale (JVM), par défaut.</li>
 *   <li>{@link RedisRateLimitStore} : partagé entre instances (Redis).</li>
 * </ul>
 * Le choix est fait dynamiquement par {@link DynamicRateLimitStore} selon le
 * réglage admin {@code rateLimiterStore} (« redis » ou « in-memory ») et la
 * disponibilité effective de Redis (avec repli automatique en mémoire).
 */
public interface RateLimitStore {

    /**
     * Consomme 1 unité du quota associé à la clé, dans une fenêtre glissante
     * de durée {@code window}.
     *
     * @param key    clé d'identification (IP + méthode + endpoint…)
     * @param limit  nombre maximal d'unités autorisées dans la fenêtre
     * @param window durée de la fenêtre
     * @return {@code true} si la requête est autorisée, {@code false} sinon
     */
    boolean tryAcquire(String key, int limit, Duration window);
}