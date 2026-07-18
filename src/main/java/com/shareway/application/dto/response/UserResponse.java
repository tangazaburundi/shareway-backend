package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id, firstName, lastName, email, phone, avatarUrl, bio, role, preferredLang;
    private boolean phoneVisible, emailVerified, phoneVerified, identityVerified, twoFaEnabled, active, blocked;
    private boolean adminApproved;
    private BigDecimal rating;
    private int reviewCount;
    private String blockReason;
    private VehicleResponse vehicle;
    private TravelPreferencesResponse preferences;
    private LocalDateTime createdAt, lastLoginAt;
    private String systemRole;
}
