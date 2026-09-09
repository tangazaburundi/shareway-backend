package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class EmergencyContactRequest {
    @NotBlank @Size(max=100) private String name;
    @NotBlank @Size(max=20) private String phone;
    @Size(max=50) private String relationship;
}