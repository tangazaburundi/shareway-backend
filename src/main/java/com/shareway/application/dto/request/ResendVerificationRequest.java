package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ResendVerificationRequest {
    @NotBlank @Email @Size(max=254) private String email;
}