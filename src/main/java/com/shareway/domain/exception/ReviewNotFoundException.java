package com.shareway.domain.exception;

public class ReviewNotFoundException extends DomainException {
    public ReviewNotFoundException(String message) {
        super("error.review.not.found", message);
    }
}
