package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAvailabilityResponse {

    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private BigDecimal rating;
    private boolean available;
    private String status;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private Short currentHeading;
    private int maxDistanceKm;
    private boolean autoAccept;
    private LocalDateTime lastLocationUpdate;
}
