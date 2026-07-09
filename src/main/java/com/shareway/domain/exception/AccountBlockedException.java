package com.shareway.domain.exception;

public class AccountBlockedException extends DomainException {
    public AccountBlockedException(String message) {
        super("error.auth.account.blocked", message);
    }
}
