package com.shareway.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour PATCH /users/me/role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchRoleRequest {

    @NotBlank
    @Pattern(regexp = "DRIVER|PASSENGER|BOTH", message = "Role must be DRIVER, PASSENGER or BOTH")
    private String role;
}
