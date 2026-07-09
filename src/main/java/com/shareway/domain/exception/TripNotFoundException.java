package com.shareway.domain.exception;

public class TripNotFoundException extends DomainException {
    public TripNotFoundException(String message) {
        super("error.trip.not.found", message);
    }
}
