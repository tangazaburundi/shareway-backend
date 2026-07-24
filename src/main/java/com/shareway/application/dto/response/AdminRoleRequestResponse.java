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
public class AdminRoleRequestResponse {
    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String currentRole;
    private String requestedRole;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
