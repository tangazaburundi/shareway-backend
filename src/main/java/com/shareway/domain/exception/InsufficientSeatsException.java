package com.shareway.domain.exception;

public class InsufficientSeatsException extends DomainException {
    public InsufficientSeatsException(String message) {
        super("error.trip.insufficient.seats", message);
    }
}
