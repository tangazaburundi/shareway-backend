package com.shareway.application.dto.response;
import lombok.*; 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token, refreshToken;
    private UserResponse user;
    private boolean requiresTwoFa;
    private String twoFaSessionToken;
}
