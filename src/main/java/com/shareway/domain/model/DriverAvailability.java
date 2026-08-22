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
@Table(name = "driver_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean isAvailable = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.OFFLINE;

    @Column(name = "current_lat", precision = 10, scale = 8)
    private BigDecimal currentLat;

    @Column(name = "current_lng", precision = 11, scale = 8)
    private BigDecimal currentLng;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    @Column(name = "current_heading")
    private Short currentHeading;

    @Column(name = "max_distance_km", nullable = false)
    @Builder.Default
    private int maxDistanceKm = 15;

    @Column(name = "auto_accept", nullable = false)
    @Builder.Default
    private boolean autoAccept = false;

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

    public void goOnline() {
        this.isAvailable = true;
        this.status = DriverStatus.AVAILABLE;
    }

    public void goOffline() {
        this.isAvailable = false;
        this.status = DriverStatus.OFFLINE;
    }

    public void setBusy() {
        this.isAvailable = false;
        this.status = DriverStatus.BUSY;
    }

    public void setOnTrip() {
        this.isAvailable = false;
        this.status = DriverStatus.ON_TRIP;
    }

    public void setAvailable() {
        this.isAvailable = true;
        this.status = DriverStatus.AVAILABLE;
    }

    public void updateLocation(BigDecimal lat, BigDecimal lng, Short heading) {
        this.currentLat = lat;
        this.currentLng = lng;
        this.currentHeading = heading;
        this.lastLocationUpdate = LocalDateTime.now();
    }

    public boolean hasValidLocation() {
        return currentLat != null && currentLng != null
                && lastLocationUpdate != null
                && lastLocationUpdate.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    public enum DriverStatus {
        OFFLINE, AVAILABLE, BUSY, ON_TRIP
    }
}
