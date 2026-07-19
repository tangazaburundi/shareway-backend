package com.shareway.domain.exception;

public class PartenaireNotFoundException extends DomainException {
    public PartenaireNotFoundException(String message) {
        super("error.partenaire.not.found", message);
    }
}
