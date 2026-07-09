package com.shareway.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ReviewCreatedEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private final String eventType = "REVIEW_CREATED";
    private final String reviewId;
    private final String targetUserId;
    private final int rating;

    public ReviewCreatedEvent(String reviewId, String targetUserId, int rating) {
        this.reviewId = reviewId;
        this.targetUserId = targetUserId;
        this.rating = rating;
    }
}
