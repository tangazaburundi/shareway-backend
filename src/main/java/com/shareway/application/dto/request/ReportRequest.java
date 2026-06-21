package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ReportRequest {
    @NotBlank private String targetType;
    @NotBlank private String targetId;
    @NotBlank private String reason;
    @Size(max=2000) private String description;
}
