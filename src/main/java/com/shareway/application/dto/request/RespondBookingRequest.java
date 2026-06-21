package com.shareway.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RespondBookingRequest {
    /** ACCEPTED ou REJECTED */
    @NotBlank
    @Pattern(regexp = "ACCEPTED|REJECTED", message = "Action must be ACCEPTED or REJECTED")
    private String action;

    /** Raison du refus (obligatoire si REJECTED) */
    private String reason;
}
