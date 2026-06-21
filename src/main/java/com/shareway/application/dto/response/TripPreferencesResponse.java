package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPreferencesResponse {
    private boolean music;
    private boolean smoking;
    private boolean pets;
    private boolean talking;
    private boolean airConditioning;
    private boolean smallLuggage;
    private boolean largeLuggage;
}
