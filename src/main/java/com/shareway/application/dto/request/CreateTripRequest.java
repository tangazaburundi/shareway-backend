package com.shareway.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {
    @NotBlank
    @Size(max = 150)
    private String departureCity;
    @NotBlank
    @Size(max = 150)
    private String arrivalCity;
    private String departureAddress, arrivalAddress;
    private java.math.BigDecimal departureLat, departureLng, arrivalLat, arrivalLng;
    @NotNull
    @Future
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    @Min(1)
    @Max(8)
    private int totalSeats;
    @NotNull
    @DecimalMin(value = "0.01", message = "Le prix par place doit être supérieur ou égal à 0.01")
    private BigDecimal pricePerSeat;
    @NotBlank
    private String currency;
    @Size(max = 2000)
    private String description;
    private List<StopPointRequest> stopPoints;
    private boolean recurring;
    private String frequency;
    private List<String> recurringDays;
    private java.time.LocalDate recurringEndDate;
    private TripPreferencesRequest preferences;
}
