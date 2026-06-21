package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {
    @Size(min=2,max=100) private String firstName;
    @Size(min=2,max=100) private String lastName;
    private String phone;
    private boolean phoneVisible;
    @Size(max=2000) private String bio;
    private String preferredLang;
    private String role;
    private TripPreferencesRequest preferences;
}
