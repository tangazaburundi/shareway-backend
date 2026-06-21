package com.shareway.infrastructure.adapter.audit.domain.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    String getEventId();

    LocalDateTime getOccurredAt();

    String getEventType();
}
