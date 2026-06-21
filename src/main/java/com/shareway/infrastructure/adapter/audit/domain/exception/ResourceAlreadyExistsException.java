package com.shareway.infrastructure.adapter.audit.domain.exception;

public class ResourceAlreadyExistsException extends DomainException {
    public ResourceAlreadyExistsException(String message) {
        super("ResourceAlreadyExistsException", message);
    }
}
