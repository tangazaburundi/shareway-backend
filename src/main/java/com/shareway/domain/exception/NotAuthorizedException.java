package com.shareway.domain.exception;

public class NotAuthorizedException extends DomainException {
    public NotAuthorizedException(String message) {
        super("error.auth.invalid.credentials", message);
    }
}
