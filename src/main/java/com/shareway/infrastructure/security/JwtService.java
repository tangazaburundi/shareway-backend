package com.shareway.infrastructure.security;

import com.shareway.application.port.out.JwtPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class JwtService implements JwtPort {

    private final SecretKey key;
    private final long expiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration:86400000}") long expiration,
            @Value("${security.jwt.refresh-expiration:604800000}") long refreshExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    @Override
    public String generateToken(String userId, String email, String role, String systemeRole) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim("type", "ACCESS")
                .claim("systemeRole", systemeRole)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key).compact();
    }


    @Override
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId).claim("type", "REFRESH")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key).compact();
    }

    @Override
    public String generateTwoFaSessionToken(String userId) {
        return Jwts.builder()
                .subject(userId).claim("type", "2FA_SESSION")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 300_000))
                .signWith(key).compact();
    }

    @Override
    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    @Override
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractRole(String token) {
        return (String) parse(token).get("role");
    }

    public String extractSystemeRole(String token) {
        return (String) parse(token).get("systemeRole");
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
