package com.shareway.infrastructure.adapter.audit.domain.model;

import com.shareway.infrastructure.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "departure_city", nullable = false)
    private String departureCity;

    @Column(name = "arrival_city", nullable = false)
    private String arrivalCity;

    @Column(name = "departure_address")
    private String departureAddress;

    @Column(name = "arrival_address")
    private String arrivalAddress;

    @Column(name = "departure_lat", precision = 10, scale = 7)
    private BigDecimal departureLat;

    @Column(name = "departure_lng", precision = 10, scale = 7)
    private BigDecimal departureLng;

    @Column(name = "arrival_lat", precision = 10, scale = 7)
    private BigDecimal arrivalLat;

    @Column(name = "arrival_lng", precision = 10, scale = 7)
    private BigDecimal arrivalLng;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "price_per_seat", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Currency currency = Currency.FBU;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.OPEN;

    @Column(name = "share_token", unique = true)
    private String shareToken;

    // Récurrence
    @Column(name = "is_recurring")
    @Builder.Default
    private boolean recurring = false;

    @Enumerated(EnumType.STRING)
    private TripFrequency frequency;

    // @JsonDeserialize(using = ArrayToStringDeserializer.class)
    //@Column(name = "recurring_days")
    //private String recurringDays;

    @Column(name = "recurring_days")
    @Convert(converter = StringListConverter.class)
    private List<String> recurringDays;

    @Column(name = "recurring_end_date")
    private LocalDate recurringEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_trip_id")
    private Trip parentTrip;

    // Géolocalisation live
    @Column(name = "current_lat", precision = 10, scale = 7)
    private BigDecimal currentLat;

    @Column(name = "current_lng", precision = 10, scale = 7)
    private BigDecimal currentLng;

    @Column(name = "tracking_enabled")
    @Builder.Default
    private boolean trackingEnabled = false;

    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private TripPreferences preferences;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    @Builder.Default
    private List<StopPoint> stopPoints = new ArrayList<>();

    @OneToMany(mappedBy = "trip", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (shareToken == null) shareToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Domain behaviors
    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    public boolean isOpen() {
        return status == TripStatus.OPEN;
    }

    public boolean canJoin(int seats) {
        return status == TripStatus.OPEN && availableSeats >= seats;
    }

    public void bookSeats(int seats) {
        if (!canJoin(seats)) throw new IllegalStateException("Not enough available seats");
        availableSeats -= seats;
        if (availableSeats == 0) status = TripStatus.FULL;
    }

    public void releaseSeats(int seats) {
        availableSeats = Math.min(availableSeats + seats, totalSeats);
        if (status == TripStatus.FULL && availableSeats > 0) status = TripStatus.OPEN;
    }

    public void cancel(String byUserId) {
        this.status = TripStatus.CANCELLED;
    }

    public void complete() {
        this.status = TripStatus.COMPLETED;
    }

    public void softDelete(String byUserId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = byUserId;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public int getFillRate() {
        if (totalSeats == 0) return 0;
        return (int) (((double) (totalSeats - availableSeats) / totalSeats) * 100);
    }

    public int getPassengerCount() {
        return totalSeats - availableSeats;
    }

    public enum TripStatus {
        OPEN, FULL, CANCELLED, COMPLETED, PENDING, REJECTED
    }

    public enum TripFrequency {ONCE, WEEKLY, BIWEEKLY, MONTHLY}

    public enum Currency {FBU, USD, EUR}
}
