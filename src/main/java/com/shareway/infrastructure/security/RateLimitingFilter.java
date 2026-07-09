package com.shareway.infrastructure.security;

import com.shareway.infrastructure.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> rateLimitBuckets;

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTER_PATH = "/auth/register";
    private static final String FORGOT_PASSWORD_PATH = "/auth/forgot-password";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Supplier<Bucket> bucketSupplier;

        if (path.equals(LOGIN_PATH)) {
            bucketSupplier = RateLimitingConfig::createLoginBucket;
        } else if (path.equals(REGISTER_PATH)) {
            bucketSupplier = RateLimitingConfig::createRegisterBucket;
        } else if (path.equals(FORGOT_PASSWORD_PATH)) {
            bucketSupplier = RateLimitingConfig::createForgotPasswordBucket;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp + ":" + path;
        Bucket bucket = rateLimitBuckets.computeIfAbsent(key, k -> bucketSupplier.get());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
