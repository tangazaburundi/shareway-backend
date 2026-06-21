package com.shareway.infrastructure.adapter.audit.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String phone;

    @Column(name = "phone_visible")
    @Builder.Default
    private boolean phoneVisible = true;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.PASSENGER;

    @Column(name = "preferred_lang")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Language preferredLang = Language.fr;

    // Account status
    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_blocked")
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "blocked_by_id")
    private String blockedById;

    // Email verification
    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verify_token")
    private String emailVerifyToken;

    @Column(name = "email_verify_expiry")
    private LocalDateTime emailVerifyExpiry;

    // Phone verification
    @Column(name = "phone_verified")
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "phone_otp")
    private String phoneOtp;

    @Column(name = "phone_otp_expiry")
    private LocalDateTime phoneOtpExpiry;

    // Identity
    @Column(name = "identity_verified")
    @Builder.Default
    private boolean identityVerified = false;

    @Column(name = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;

    @Column(name = "identity_verified_by")
    private String identityVerifiedBy;

    // 2FA
    @Column(name = "two_fa_enabled")
    @Builder.Default
    private boolean twoFaEnabled = false;

    @Column(name = "two_fa_secret")
    private String twoFaSecret;

    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    // Stats
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Relations
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserTravelPreferences travelPreferences;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private int failedLoginAttempts = 0;
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Transient
    private SystemRole systemRole;

// ── Méthodes métier à ajouter ─────────────────────────────────────────

    // Domain behaviors
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void registerFailedLogin(int maxAttempts, int lockMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(String byUserId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = byUserId;
        this.active = false;
    }

    public void block(String reason, String byUserId) {
        this.blocked = true;
        this.blockReason = reason;
        this.blockedAt = LocalDateTime.now();
        this.blockedById = byUserId;
        this.active = false;
    }

    public void unblock() {
        this.blocked = false;
        this.blockReason = null;
        this.blockedAt = null;
        this.blockedById = null;
        this.active = true;
    }

    public void verifyEmail() {

        this.emailVerified = true;
        this.emailVerifyToken = null;
        this.emailVerifyExpiry = null;
    }

    public void verifyIdentity(String adminId) {

        this.identityVerified = true;
        this.identityVerifiedAt = LocalDateTime.now();
        this.identityVerifiedBy = adminId;
    }

    public void updateRating(int newRating) {
        java.math.BigDecimal total = this.rating.multiply(java.math.BigDecimal.valueOf(this.reviewCount))
                .add(java.math.BigDecimal.valueOf(newRating));
        this.reviewCount++;
        this.rating = total.divide(java.math.BigDecimal.valueOf(this.reviewCount), 2, java.math.RoundingMode.HALF_UP);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isDriver() {
        return role == UserRole.DRIVER || role == UserRole.BOTH;
    }

    public boolean isPassenger() {
        return role == UserRole.PASSENGER || role == UserRole.BOTH;
    }

    public enum UserRole {DRIVER, PASSENGER, BOTH}

    public enum Language {fr, ki}

}
