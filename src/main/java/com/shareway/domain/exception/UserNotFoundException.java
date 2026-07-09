package com.shareway.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String message) {
        super("error.user.not.found", message);
    }
}
