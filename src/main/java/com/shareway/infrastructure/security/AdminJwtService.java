package com.shareway.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

/**
 * JWT dédié aux sessions admin.
 * <p>
 * Sécurité :
 * - clé de signature DISTINCTE de celle des utilisateurs normaux
 * (compromission du JWT user n'impacte pas l'admin et vice-versa)
 * - durée de vie courte (15-30 min) + refresh token séparé
 * - claim "type": "ADMIN_ACCESS" vérifié explicitement (anti rejouabilité
 * d'un token user sur les routes admin)
 * - claims role + permissions embarqués pour autorisation fine côté front
 */
@Service
public class AdminJwtService {

    private final SecretKey key;
    private final long accessTtlMs;
    private final long refreshTtlMs;
    private final long twoFaSessionTtlMs = 5 * 60 * 1000L; // 5 min

    public AdminJwtService(
            @Value("${security.admin-jwt.secret}") String secret,
            @Value("${security.admin-jwt.access-expiration:1800000}") long accessTtlMs,   // 30 min
            @Value("${security.admin-jwt.refresh-expiration:43200000}") long refreshTtlMs  // 12h
    ) {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException("security.admin-jwt.secret must be at least 32 chars");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }

    public String generateAccessToken(String userId, String email, String role, Set<String> permissions) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", "ADMIN_ACCESS")
                .claim("email", email)
                .claim("role", role)
                .claim("perms", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTtlMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", "ADMIN_REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTtlMs))
                .signWith(key)
                .compact();
    }

    public String generateTwoFaSessionToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", "ADMIN_2FA_SESSION")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + twoFaSessionTtlMs))
                .signWith(key)
                .compact();
    }

    public long accessTtlSeconds() {
        return accessTtlMs / 1000;
    }

    /**
     * Valide et retourne les claims, ou lève JwtException si invalide/expiré
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token, String expectedType) {
        try {
            Claims c = parse(token);
            return expectedType.equals(c.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) parse(token).get("role");
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractPermissions(String token) {
        Object p = parse(token).get("perms");
        return p instanceof Set ? (Set<String>) p : Set.of();
    }
}