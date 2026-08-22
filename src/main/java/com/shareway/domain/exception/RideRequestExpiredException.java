package com.shareway.domain.exception;

public class RideRequestExpiredException extends DomainException {
    public RideRequestExpiredException(String message) {
        super("error.ride.request.expired", message);
    }
}
