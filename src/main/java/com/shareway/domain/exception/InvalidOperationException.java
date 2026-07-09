package com.shareway.domain.exception;

public class InvalidOperationException extends DomainException {
    public InvalidOperationException(String message) {
        super("error.operation.invalid", message);
    }
}
