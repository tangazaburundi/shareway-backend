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
public class PricingConfigResponse {

    private String id;
    private String name;
    private String currency;
    private BigDecimal basePrice;
    private BigDecimal pricePerKm;
    private BigDecimal pricePerMin;
    private BigDecimal minimumPrice;
    private BigDecimal surgeMultiplier;
    private int surgeThreshold;
    private BigDecimal platformFeePercent;
    private int freeCancellationMinutes;
    private boolean active;
}
