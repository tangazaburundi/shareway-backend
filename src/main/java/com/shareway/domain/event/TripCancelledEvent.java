package com.shareway.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class TripCancelledEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private final String eventType = "TRIP_CANCELLED";
    private final String tripId;
    private final String driverId;
    private final String reason;
    private final int affectedPassengers;

    public TripCancelledEvent(String tripId, String driverId, String reason, int affectedPassengers) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.reason = reason;
        this.affectedPassengers = affectedPassengers;
    }
}
