package com.shareway.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour PATCH/PUT /users/me
 * Tous les champs sont optionnels (Partial<User> côté Angular).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(min = 2, max = 100)
    private String firstName;

    @Size(min = 2, max = 100)
    private String lastName;

    @Size(max = 30)
    private String phone;

    private Boolean phoneVisible;

    @Size(max = 2000)
    private String bio;

    /** "fr" ou "ki" */
    private String preferredLang;

    /** Préférences de voyage */
    private PreferencesPayload preferences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferencesPayload {
        private Boolean music;
        private Boolean smoking;
        private Boolean pets;
        private Boolean talking;
    }
}
