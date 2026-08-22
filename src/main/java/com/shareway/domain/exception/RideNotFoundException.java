package com.shareway.domain.exception;

public class RideNotFoundException extends DomainException {
    public RideNotFoundException(String message) {
        super("error.ride.not.found", message);
    }
}
