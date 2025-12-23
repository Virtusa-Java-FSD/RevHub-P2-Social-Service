package com.revhub.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountStatusException extends AuthenticationException {
    public AccountStatusException(String message) {
        super(message);
    }
}
