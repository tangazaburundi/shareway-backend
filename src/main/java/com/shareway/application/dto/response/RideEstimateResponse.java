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
public class RideEstimateResponse {

    private BigDecimal distanceKm;
    private int durationMin;
    private BigDecimal estimatedPrice;
    private BigDecimal surgeMultiplier;
    private String currency;
    private BigDecimal basePrice;
    private BigDecimal pricePerKm;
    private BigDecimal pricePerMin;
    private BigDecimal platformFeePercent;
    private boolean surgeActive;
    private int nearbyDriversCount;
}
