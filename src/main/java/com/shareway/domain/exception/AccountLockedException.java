package com.shareway.domain.exception;

public class AccountLockedException extends DomainException {
    public AccountLockedException(String message) {
        super("error.auth.account.locked", message);
    }
}
