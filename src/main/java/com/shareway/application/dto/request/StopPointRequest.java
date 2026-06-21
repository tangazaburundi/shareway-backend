package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor
public class StopPointRequest {
    @NotBlank private String city;
    private String address;
    private java.math.BigDecimal lat, lng;
    @Min(0) private int order;
    private LocalDateTime arrivalTime;
}
