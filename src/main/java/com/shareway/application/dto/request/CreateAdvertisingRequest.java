package com.shareway.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdvertisingRequest {
    @NotBlank
    private String title;

    private String description;
    private String imageUrl;
    private String linkUrl;

    @NotBlank
    private String position;

    private String displayStart;
    private String displayEnd;

    @PositiveOrZero
    private int sortOrder;

    private String targetAudience;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String paymentCurrency;
}
