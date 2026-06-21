package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateReviewRequest {
    @NotBlank private String tripId;
    @NotBlank private String targetUserId;
    @Min(1) @Max(5) private int rating;
    @Size(max=2000) private String comment;
}
