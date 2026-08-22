package com.shareway.domain.exception;

public class InvalidRideStateException extends DomainException {
    public InvalidRideStateException(String message) {
        super("error.ride.invalid.state", message);
    }
}
