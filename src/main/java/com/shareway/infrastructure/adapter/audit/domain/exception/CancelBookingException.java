package com.shareway.infrastructure.adapter.audit.domain.exception;

public class CancelBookingException extends DomainException {
    public CancelBookingException(String message) {
        super("CancelBookingException", message);
    }
}
