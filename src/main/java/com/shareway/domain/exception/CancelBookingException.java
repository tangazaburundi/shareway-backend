package com.shareway.domain.exception;

public class CancelBookingException extends DomainException {
    public CancelBookingException(String message) {
        super("error.trip.cancellation.invalid", message);
    }
}
