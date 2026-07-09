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
public class AdvertisingResponse {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String linkUrl;
    private String position;
    private boolean active;
    private LocalDateTime displayStart;
    private LocalDateTime displayEnd;
    private int sortOrder;
    private String targetAudience;
    private int clicks;
    private int impressions;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String paymentCurrency;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
