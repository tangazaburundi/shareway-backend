package com.shareway.application.dto.response;

import lombok.*;
import java.math.BigDecimal;

/** Profil passager visible par le conducteur */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PassengerPublicResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private BigDecimal rating;
    private int reviewCount;
    private boolean identityVerified;
    private String phone;
}
