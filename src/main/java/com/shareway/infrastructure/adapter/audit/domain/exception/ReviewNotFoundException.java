package com.shareway.infrastructure.adapter.audit.domain.exception;

public class ReviewNotFoundException extends DomainException {
    public ReviewNotFoundException(String message) {
        super("ReviewNotFoundException", message);
    }
}
