package com.shareway.application.port.out;

public interface JwtPort {
    String generateToken(String userId, String email, String role, String systemeRole);

    String generateRefreshToken(String userId);

    String generateTwoFaSessionToken(String userId);

    String extractUserId(String token);

    boolean isValid(String token);
}
