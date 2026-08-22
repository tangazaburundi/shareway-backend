package com.shareway.domain.exception;

public class DriverNotAvailableException extends DomainException {
    public DriverNotAvailableException(String message) {
        super("error.driver.not.available", message);
    }
}
