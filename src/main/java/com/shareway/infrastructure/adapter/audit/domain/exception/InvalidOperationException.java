package com.shareway.infrastructure.adapter.audit.domain.exception;

public class InvalidOperationException extends DomainException {
    public InvalidOperationException(String message) {
        super("InvalidOperationException", message);
    }
}
