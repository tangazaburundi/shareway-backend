package com.shareway.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRoleRequest {
    @NotBlank(message = "Le rôle demandé est requis")
    @Pattern(regexp = "DRIVER|PASSENGER|BOTH", message = "Le rôle doit être DRIVER, PASSENGER ou BOTH")
    private String requestedRole;

    private String reason;
}
