package com.shareway.domain.exception;

public class AdvertisingNotFoundException extends DomainException {
    public AdvertisingNotFoundException(String message) {
        super("error.advertising.not.found", message);
    }
}
