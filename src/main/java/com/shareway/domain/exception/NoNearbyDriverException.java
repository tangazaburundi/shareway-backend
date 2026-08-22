package com.shareway.domain.exception;

public class NoNearbyDriverException extends DomainException {
    public NoNearbyDriverException(String message) {
        super("error.no.nearby.driver", message);
    }
}
