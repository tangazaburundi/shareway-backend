package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponse {

    private String id;

    // Passenger info
    private String passengerId;
    private String passengerFirstName;
    private String passengerLastName;
    private String passengerAvatarUrl;
    private BigDecimal passengerRating;

    // Driver info (nullable if SEARCHING)
    private String driverId;
    private String driverFirstName;
    private String driverLastName;
    private String driverAvatarUrl;
    private BigDecimal driverRating;
    private String driverPhone;
    private String driverLicenseId;
    private String driverVehicleBrand;
    private String driverVehicleModel;
    private String driverVehicleColor;
    private String driverVehiclePlate;

    // Positions
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private String pickupAddress;
    private BigDecimal destinationLat;
    private BigDecimal destinationLng;
    private String destinationAddress;

    // Estimations
    private BigDecimal estimatedDistanceKm;
    private Integer estimatedDurationMin;
    private BigDecimal estimatedPrice;
    private BigDecimal finalPrice;
    private String currency;

    // Status
    private String status;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime driverNotifiedAt;
    private LocalDateTime driverRespondedAt;
    private LocalDateTime pickupAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Payment
    private String paymentStatus;
    private BigDecimal platformFeeAmount;
    private BigDecimal driverEarnings;

    // Meta
    private String notes;
    private int passengerCount;
    private String cancelReason;
    private String cancelledBy;

    // Surge
    private BigDecimal surgeMultiplier;

    // Rejection info
    private boolean rejectedByDriver;
    private String rejectionReason;
    private LocalDateTime rejectedAt;
}
