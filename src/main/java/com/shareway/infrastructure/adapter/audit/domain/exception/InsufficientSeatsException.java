package com.shareway.infrastructure.adapter.audit.domain.exception;

public class InsufficientSeatsException extends DomainException {
    public InsufficientSeatsException(String message) {
        super("InsufficientSeatsException", message);
    }
}
