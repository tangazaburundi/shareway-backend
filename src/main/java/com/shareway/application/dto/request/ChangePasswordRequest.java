package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank private String currentPassword;
    @NotBlank @Size(min=8)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$",
             message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un symbole")
    private String newPassword;
}
