package com.shareway.application.port.out;
public interface AuditPort {
    void log(String action, String entityType, String entityId, Object oldValue, Object newValue, String actorId);
    void logLogin(String userId, String email, String ip, String userAgent, boolean success, String failReason);
}
