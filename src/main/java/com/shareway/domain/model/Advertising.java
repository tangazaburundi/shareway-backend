package com.shareway.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "advertising")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advertising {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "link_url")
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdvertisingPosition position = AdvertisingPosition.SIDEBAR_MIDDLE;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = false;

    @Column(name = "display_start")
    private LocalDateTime displayStart;

    @Column(name = "display_end")
    private LocalDateTime displayEnd;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "target_audience")
    @Builder.Default
    private String targetAudience = "ALL";

    @Builder.Default
    private int clicks = 0;

    @Builder.Default
    private int impressions = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.FREE;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_currency")
    @Builder.Default
    private String paymentCurrency = "FBU";

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void incrementClicks() {
        this.clicks++;
    }

    public void incrementImpressions() {
        this.impressions++;
    }

    public boolean isCurrentlyDisplayable() {
        if (!active) return false;
        LocalDateTime now = LocalDateTime.now();
        if (displayStart != null && now.isBefore(displayStart)) return false;
        if (displayEnd != null && now.isAfter(displayEnd)) return false;
        return true;
    }

    public enum AdvertisingPosition {
        SIDEBAR_TOP, SIDEBAR_MIDDLE, SIDEBAR_BOTTOM, TOP_BANNER, BOTTOM_BANNER, POPUP
    }

    public enum PaymentStatus {
        PENDING, PAID, FREE
    }
}
