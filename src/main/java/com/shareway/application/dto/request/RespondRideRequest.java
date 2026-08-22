package com.shareway.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondRideRequest {

    @NotNull
    private String action; // ACCEPTED or REJECTED

    private String reason;
}
