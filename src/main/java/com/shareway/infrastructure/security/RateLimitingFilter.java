package com.shareway.infrastructure.security;

import com.shareway.infrastructure.security.ratelimit.DynamicRateLimitStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final DynamicRateLimitStore rateLimitStore;

    /**
     * Le header X-Forwarded-For n'est pris en compte QUE si l'application
     * est réellement derrière un reverse proxy de confiance qui le fixe.
     * Sinon, un attaquant peut le falsifier à chaque requête pour
     * contourner entièrement le rate limiting (nouvelle clé = nouveau bucket).
     */
    @Value("${shareway.app.trusted-forward-headers:false}")
    private boolean trustedForwardHeaders;

    /** Règle de rate limiting : nombre max d'appels par fenêtre (secondes). */
    private record RateRule(int limit, int windowSeconds) {}

    private static final Map<String, RateRule> POST_RULES = new ConcurrentHashMap<>();
    private static final Map<String, RateRule> GET_RULES = new ConcurrentHashMap<>();

    static {
        POST_RULES.put("/auth/login", new RateRule(5, 60));
        POST_RULES.put("/auth/register", new RateRule(3, 60));
        POST_RULES.put("/auth/forgot-password", new RateRule(3, 60));
        POST_RULES.put("/rides", new RateRule(20, 60));
        POST_RULES.put("/messages", new RateRule(30, 60));
        GET_RULES.put("/rides/nearby", new RateRule(60, 60));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        RateRule rule = resolveRule(method, path);

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + method + ":" + path;
        boolean allowed = rateLimitStore.tryAcquire(key, rule.limit(), Duration.ofSeconds(rule.windowSeconds()));

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private RateRule resolveRule(String method, String path) {
        if ("POST".equalsIgnoreCase(method)) {
            return POST_RULES.get(path);
        } else if ("GET".equalsIgnoreCase(method)) {
            return GET_RULES.get(path);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (trustedForwardHeaders) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isBlank()) {
                String ip = xfHeader.split(",")[0].trim();
                if (!ip.isBlank()) return ip;
            }
        }
        return request.getRemoteAddr();
    }
}