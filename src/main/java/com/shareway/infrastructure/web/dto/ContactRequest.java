package com.shareway.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequest {

    @NotBlank
    @Size(max = 200)
    private String nom;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 100)
    private String sujet;

    @NotBlank
    @Size(max = 2000)
    private String message;
}
