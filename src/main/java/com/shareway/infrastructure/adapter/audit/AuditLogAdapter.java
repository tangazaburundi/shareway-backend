package com.shareway.infrastructure.adapter.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shareway.application.port.out.AuditPort;
import com.shareway.infrastructure.adapter.audit.domain.model.AuditLog;
import com.shareway.infrastructure.adapter.audit.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogAdapter implements AuditPort {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void log(String action, String entityType, String entityId,
                    Object oldValue, Object newValue, String actorId) {
        try {
            AuditLog audit = AuditLog.builder()
                    .actorId(actorId)
                    .action(action).entityType(entityType).entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .success(true)
                    .build();
            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to save audit log for action {}: {}", action, e.getMessage());
        }
    }

    @Override
    @Async
    public void logLogin(String userId, String email, String ip,
                         String userAgent, boolean success, String failReason) {
        try {
            AuditLog audit = AuditLog.builder()
                    .actorId(userId).actorEmail(email)
                    .action(success ? "LOGIN_SUCCESS" : "LOGIN_FAILED")
                    .entityType("User").entityId(userId)
                    .ipAddress(ip).userAgent(userAgent)
                    .success(success).errorMessage(failReason)
                    .build();
            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to save login audit: {}", e.getMessage());
        }
    }
}
