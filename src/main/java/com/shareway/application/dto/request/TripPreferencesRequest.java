package com.shareway.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripPreferencesRequest {
    private boolean music;
    private boolean smoking;
    private boolean pets;           // animaux autorisés
    private boolean talking;
    private boolean airConditioning;
    private boolean smallLuggage;   // petite valise acceptée
    private boolean largeLuggage;   // grande valise acceptée
}
