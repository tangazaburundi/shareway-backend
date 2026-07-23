package com.shareway.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modification d'un trajet existant.
 * Tous les champs sont optionnels — seuls les champs fournis sont mis à jour.
 * Si des passagers ont réservé, une notification leur est envoyée.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateTripRequest {

    @Size(max = 150)
    private String departureCity;

    @Size(max = 150)
    private String arrivalCity;

    private String departureAddress;
    private String arrivalAddress;
    private BigDecimal departureLat;
    private BigDecimal departureLng;
    private BigDecimal arrivalLat;
    private BigDecimal arrivalLng;

    @Future
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    @Min(1) @Max(8)
    private Integer totalSeats;

    @DecimalMin(value = "0.01", message = "Le prix par place doit être supérieur ou égal à 0.01")
    private BigDecimal pricePerSeat;

    private String currency;

    @Size(max = 2000)
    private String description;

    private TripPreferencesRequest preferences;
    private List<StopPointRequest>  stopPoints;

    /** Message envoyé aux passagers pour expliquer la modification */
    @Size(max = 500)
    private String notificationMessage;
}
