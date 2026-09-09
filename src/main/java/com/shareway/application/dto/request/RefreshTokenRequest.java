package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank @Size(max=2048) private String refreshToken;
}