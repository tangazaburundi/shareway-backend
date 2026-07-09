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
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(name = "seats_booked")
    @Builder.Default
    private int seatsBooked = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "driver_reject_reason")
    private String driverRejectReason;

    @Column(name = "driver_response_at")
    private LocalDateTime driverResponseAt;

    @Column(name = "notify_driver")
    @Builder.Default
    private boolean notifyDriver = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Trip.Currency currency = Trip.Currency.FBU;

    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "stripe_status")
    private String stripeStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    // ── Méthodes métier ───────────────────────────────────────────────────

    /**
     * Conducteur accepte
     */
    public void confirm() {
        this.status = BookingStatus.CONFIRMED;
        this.driverResponseAt = LocalDateTime.now();
    }

    /**
     * Conducteur refuse
     */
    public void reject(String reason) {
        this.status = BookingStatus.REJECTED;
        this.driverRejectReason = reason;
        this.driverResponseAt = LocalDateTime.now();
        // Remettre les places disponibles
        if (this.trip != null) this.trip.releaseSeats(this.seatsBooked);
    }

    /**
     * Passager annule (PENDING ou CONFIRMED)
     */
    public void cancelByPassenger(String reason) {
        this.status = BookingStatus.CANCELLED;
        this.cancelReason = reason;
        if (this.trip != null) this.trip.releaseSeats(this.seatsBooked);
    }

    /**
     * Fin de trajet
     */
    public void complete() {
        this.status = BookingStatus.COMPLETED;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    /**
     * Réservation "active" = PENDING ou CONFIRMED
     */
    public boolean isActive() {
        return status == BookingStatus.PENDING ||
                status == BookingStatus.CONFIRMED ||
                status == BookingStatus.COMPLETED;
    }

    public enum BookingStatus {PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED}
}
