package com.shareway.domain.exception;

public class AccountPermanentlyLockedException extends DomainException {
    public AccountPermanentlyLockedException(String message) {
        super("error.auth.account.locked.permanent", message);
    }
}
