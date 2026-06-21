package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {
    private String token;
    private String refreshToken;
    private long expiresIn;        // secondes
    private AdminProfile admin;
    private boolean requiresTwoFa;
    private String twoFaSessionToken;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminProfile {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String role;          // SUPER_ADMIN / ADMIN / MODERATOR / SUPPORT
        private Set<String> permissions;
    }
}