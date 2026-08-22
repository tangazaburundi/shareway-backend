package com.shareway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int currentUses;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(length = 10)
    private String currency;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
                && now.isAfter(validFrom)
                && now.isBefore(validUntil)
                && currentUses < maxUses;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid()) return BigDecimal.ZERO;
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue).divide(new BigDecimal("100"));
        } else {
            discount = discountValue;
        }

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        return discount;
    }
}
