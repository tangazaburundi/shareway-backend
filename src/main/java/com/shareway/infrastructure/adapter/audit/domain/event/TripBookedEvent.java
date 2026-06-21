package com.shareway.infrastructure.adapter.audit.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class TripBookedEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private final String eventType = "TRIP_BOOKED";
    private final String tripId;
    private final String passengerId;
    private final String driverId;
    private final int seatsBooked;

    public TripBookedEvent(String tripId, String passengerId, String driverId, int seatsBooked) {
        this.tripId = tripId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.seatsBooked = seatsBooked;
    }
}
