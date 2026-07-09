package com.shareway.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class UserBlockedEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private final String eventType = "USER_BLOCKED";
    private final String userId;
    private final String adminId;
    private final String reason;

    public UserBlockedEvent(String userId, String adminId, String reason) {
        this.userId = userId;
        this.adminId = adminId;
        this.reason = reason;
    }
}
