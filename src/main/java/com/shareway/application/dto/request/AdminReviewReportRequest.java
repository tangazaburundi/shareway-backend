package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class AdminReviewReportRequest {
    @NotBlank private String status; // REVIEWED, DISMISSED, ACTIONED
    @Size(max=1000) private String actionTaken;
}
