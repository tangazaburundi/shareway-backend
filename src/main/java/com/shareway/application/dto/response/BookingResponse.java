package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String id;
    private String tripId;
    private String tripStatus;
    private String driverId;
    private String status;          // PENDING / CONFIRMED / REJECTED / CANCELLED / COMPLETED
    private int seatsBooked;
    private BigDecimal amountPaid;
    private String currency;
    private String cancelReason;
    private String driverRejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime driverResponseAt;

    // Info trajet résumée
    private String departureCity;
    private String arrivalCity;
    private LocalDateTime departureTime;
    private BigDecimal pricePerSeat;

    // Info passager (vue conducteur)
    private PassengerPublicResponse passenger;
}
