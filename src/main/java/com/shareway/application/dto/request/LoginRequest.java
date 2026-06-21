package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank @Size(min=6) private String password;
    private String twoFaCode;
}
