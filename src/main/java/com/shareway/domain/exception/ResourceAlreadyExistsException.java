package com.shareway.domain.exception;

public class ResourceAlreadyExistsException extends DomainException {
    public ResourceAlreadyExistsException(String message) {
        super("error.resource.already.exists", message);
    }
}
