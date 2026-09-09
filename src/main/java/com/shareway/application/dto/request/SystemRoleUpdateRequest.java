package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class SystemRoleUpdateRequest {
    @NotBlank @Size(max=50) private String systemRole;
}