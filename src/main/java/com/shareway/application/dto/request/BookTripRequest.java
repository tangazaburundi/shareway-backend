package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class BookTripRequest {
    @Min(1) @Max(8) private int seats = 1;
    private String currency;
    private String couponCode;
}
