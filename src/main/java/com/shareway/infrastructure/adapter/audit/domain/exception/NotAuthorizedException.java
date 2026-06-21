package com.shareway.infrastructure.adapter.audit.domain.exception;

public class NotAuthorizedException extends DomainException {
    public NotAuthorizedException(String message) {
        super("NotAuthorizedException", message);
    }
}
