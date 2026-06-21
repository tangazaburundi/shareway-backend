package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class TwoFaVerifyRequest {
    @NotBlank @Size(min=6,max=8) private String code;
}
