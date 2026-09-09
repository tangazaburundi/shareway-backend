package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ReasonRequest {
    @Size(max=500) private String reason;
}