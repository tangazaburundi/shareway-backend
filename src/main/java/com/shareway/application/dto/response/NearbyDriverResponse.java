package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyDriverResponse {

    private String userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private BigDecimal rating;
    private int reviewCount;
    private BigDecimal distanceKm;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleColor;
}
