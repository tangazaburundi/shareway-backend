package com.shareway.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour PUT /users/me/vehicle
 * Correspond à Partial<Vehicle> côté Angular.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveVehicleRequest {

    @NotBlank
    @Size(max = 100)
    private String brand;

    @NotBlank
    @Size(max = 100)
    private String model;

    @NotBlank
    @Size(max = 50)
    private String color;

    @NotBlank
    @Size(max = 20)
    private String licensePlate;

    private Short year;
}
