package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class DeviceTokenRequest {
    @NotBlank @Size(max=512) private String token;
    @Size(max=20) private String platform;
}