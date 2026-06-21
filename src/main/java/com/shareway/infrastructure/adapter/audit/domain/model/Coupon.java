package com.shareway.infrastructure.adapter.audit.domain.model;

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
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false, unique = true)
    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_trip_amount", precision = 10, scale = 2)
    private BigDecimal minTripAmount;
    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;
    @Column(name = "max_uses")
    private Integer maxUses;
    @Column(name = "current_uses")
    @Builder.Default
    private int currentUses = 0;
    @Column(name = "max_uses_per_user")
    @Builder.Default
    private int maxUsesPerUser = 1;
    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
    @Column(name = "starts_at")
    private LocalDateTime startsAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at", updatable = false)
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

    public enum DiscountType {PERCENT, FIXED}
}
