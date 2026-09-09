package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ContentRequest {
    @NotBlank @Size(max=5000) private String content;
}