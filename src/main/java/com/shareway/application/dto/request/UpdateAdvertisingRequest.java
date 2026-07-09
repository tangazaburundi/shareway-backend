package com.shareway.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdvertisingRequest {
    private String title;
    private String description;
    private String imageUrl;
    private String linkUrl;
    private String position;
    private String displayStart;
    private String displayEnd;
    private Integer sortOrder;
    private String targetAudience;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String paymentCurrency;
}
