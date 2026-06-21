package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class CancelBookingRequest {
    @NotBlank private String reason;
    private boolean notifyDriver = true;
}
