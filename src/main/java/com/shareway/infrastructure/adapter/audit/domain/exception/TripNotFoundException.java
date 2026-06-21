package com.shareway.infrastructure.adapter.audit.domain.exception;

public class TripNotFoundException extends DomainException {
    public TripNotFoundException(String message) {
        super("TripNotFoundException", message);
    }
}
