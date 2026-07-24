package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestResponse {
    private String id;
    private String requestedRole;
    private String currentRole;
    private String status;
    private String reason;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
