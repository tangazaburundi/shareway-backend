package com.shareway.application.dto.response;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogResponse {
    private String id, action, entityType, entityId, actorEmail, actorRole, ipAddress;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
}
