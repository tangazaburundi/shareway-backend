package com.shareway.domain.exception;

public class BookingNotFoundException extends DomainException {
    public BookingNotFoundException(String message) {
        super("error.booking.not.found", message);
    }
}
