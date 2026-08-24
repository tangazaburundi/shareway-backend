package com.shareway.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "ride_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    // Pickup
    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "pickup_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal pickupLng;

    // Destination
    @Column(name = "destination_address")
    private String destinationAddress;

    @Column(name = "destination_lat", nullable = false, precision = 10, scale = 8)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", nullable = false, precision = 11, scale = 8)
    private BigDecimal destinationLng;

    // Estimations
    @Column(name = "estimated_distance_km", precision = 6, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "estimated_price", precision = 10, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Trip.Currency currency = Trip.Currency.FBU;

    // Statut
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RideStatus status = RideStatus.SEARCHING;

    // Annulation
    @Column(name = "cancelled_by")
    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    // Timestamps
    @Column(name = "search_started_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime searchStartedAt = LocalDateTime.now();

    @Column(name = "search_timeout_at")
    private LocalDateTime searchTimeoutAt;

    @Column(name = "driver_notified_at")
    private LocalDateTime driverNotifiedAt;

    @Column(name = "driver_responded_at")
    private LocalDateTime driverRespondedAt;

    @Column(name = "pickup_at")
    private LocalDateTime pickupAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Paiement
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Frais
    @Column(name = "platform_fee_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal platformFeePercent = new BigDecimal("15.00");

    @Column(name = "platform_fee_amount", precision = 10, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "driver_earnings", precision = 10, scale = 2)
    private BigDecimal driverEarnings;

    // Metadata
    @Column(length = 500)
    private String notes;

    @Column(name = "passenger_count", nullable = false)
    @Builder.Default
    private int passengerCount = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "rendered_at")
    private LocalDateTime renderedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Domain behaviors ─────────────────────────────────────────────

    public void accept(User driver) {
        this.driver = driver;
        this.status = RideStatus.ACCEPTED;
        this.driverRespondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.driver = null;
        this.status = RideStatus.SEARCHING;
        this.driverRespondedAt = LocalDateTime.now();
    }

    public void driverEnRoute() {
        this.status = RideStatus.DRIVER_EN_ROUTE;
    }

    public void driverArrived() {
        this.status = RideStatus.ARRIVED;
        this.pickupAt = LocalDateTime.now();
    }

    public void start() {
        this.status = RideStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(BigDecimal finalPrice) {
        this.status = RideStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.finalPrice = finalPrice;
        calculateFees();
    }

    public void cancelByPassenger(String reason) {
        this.status = RideStatus.CANCELLED;
        this.cancelledBy = CancelledBy.PASSENGER;
        this.cancelReason = reason;
    }

    public void cancelByDriver(String reason) {
        this.status = RideStatus.CANCELLED;
        this.cancelledBy = CancelledBy.DRIVER;
        this.cancelReason = reason;
    }

    public void expire() {
        this.status = RideStatus.EXPIRED;
    }

    public void render() {
        this.status = RideStatus.RENDERED;
        this.renderedAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = RideStatus.ARCHIVED;
        this.archivedAt = LocalDateTime.now();
    }

    private void calculateFees() {
        if (finalPrice == null) return;
        this.platformFeeAmount = finalPrice.multiply(platformFeePercent)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        this.driverEarnings = finalPrice.subtract(platformFeeAmount);
    }

    public boolean isActive() {
        return status == RideStatus.SEARCHING
                || status == RideStatus.DRIVER_FOUND
                || status == RideStatus.ACCEPTED
                || status == RideStatus.DRIVER_EN_ROUTE
                || status == RideStatus.ARRIVED
                || status == RideStatus.IN_PROGRESS;
    }

    public boolean canBeCancelledByPassenger() {
        return status == RideStatus.SEARCHING
                || status == RideStatus.DRIVER_FOUND
                || status == RideStatus.ACCEPTED
                || status == RideStatus.DRIVER_EN_ROUTE;
    }

    public boolean canBeCancelledByDriver() {
        return status == RideStatus.ACCEPTED
                || status == RideStatus.DRIVER_EN_ROUTE;
    }

    public enum RideStatus {
        SEARCHING, DRIVER_FOUND, ACCEPTED, DRIVER_EN_ROUTE, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED, EXPIRED, RENDERED, ARCHIVED
    }

    public enum CancelledBy {
        PASSENGER, DRIVER
    }

    public enum PaymentStatus {
        PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED
    }
}
