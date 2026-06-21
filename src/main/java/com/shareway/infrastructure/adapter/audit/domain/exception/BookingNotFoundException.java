package com.shareway.infrastructure.adapter.audit.domain.exception;

public class BookingNotFoundException extends DomainException {
    public BookingNotFoundException(String message) {
        super("BookingNotFoundException", message);
    }
}
