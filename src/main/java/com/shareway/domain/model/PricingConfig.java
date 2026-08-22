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
@Table(name = "pricing_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Trip.Currency currency = Trip.Currency.FBU;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "price_per_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKm;

    @Column(name = "price_per_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerMin;

    @Column(name = "minimum_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumPrice;

    @Column(name = "surge_multiplier", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal surgeMultiplier = BigDecimal.ONE;

    @Column(name = "surge_threshold", nullable = false)
    @Builder.Default
    private int surgeThreshold = 5;

    @Column(name = "platform_fee_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal platformFeePercent = new BigDecimal("15.00");

    @Column(name = "free_cancellation_minutes", nullable = false)
    @Builder.Default
    private int freeCancellationMinutes = 2;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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
}
