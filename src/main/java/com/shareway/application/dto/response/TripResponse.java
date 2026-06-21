package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private String id, departureCity, arrivalCity, departureAddress, arrivalAddress;
    private String description, status, shareToken, frequency, currency;
    private TripUserResponse driver;
    private LocalDateTime departureTime, arrivalTime, createdAt;
    private int availableSeats, totalSeats;
    private BigDecimal pricePerSeat;
    private boolean recurring;
    private List<StopPointResponse> stopPoints;
    private List<PassengerResponse> passengers;
    private TripPreferencesResponse preferences;
    private List<String> recurringDays;
}
