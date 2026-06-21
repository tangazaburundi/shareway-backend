package com.shareway.infrastructure.adapter.audit.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String message) {
        super("UserNotFoundException", message);
    }
}
