package com.shareway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @Column(name = "referral_code", nullable = false, unique = true, length = 20)
    private String referralCode;

    @Column(name = "referred_email")
    private String referredEmail;

    @Column(name = "referred_user_id")
    private String referredUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "reward_amount")
    private BigDecimal rewardAmount;

    @Column(name = "reward_currency")
    private String rewardCurrency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (referralCode == null) {
            referralCode = generateCode();
        }
    }

    private String generateCode() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public void complete(String referredUserId) {
        this.referredUserId = referredUserId;
        this.status = ReferralStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.rewardAmount = new BigDecimal("5.00");
        this.rewardCurrency = "EUR";
    }

    public enum ReferralStatus { PENDING, COMPLETED, EXPIRED }
}
