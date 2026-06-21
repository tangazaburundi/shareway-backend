package com.shareway.infrastructure.adapter.audit.domain.exception;

public class AccountBlockedException extends DomainException {
    public AccountBlockedException(String message) {
        super("AccountBlockedException", message);
    }
}
