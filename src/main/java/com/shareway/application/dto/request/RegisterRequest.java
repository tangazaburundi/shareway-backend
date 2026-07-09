package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank @Size(min=2,max=100) private String firstName;
    @NotBlank @Size(min=2,max=100) private String lastName;
    @NotBlank @Email private String email;
    @NotBlank @Size(min=8,max=100) private String password;
    private String phone;
    @NotBlank private String role; // DRIVER, PASSENGER, BOTH
    private String preferredLang;
    private String referralCode;
}
