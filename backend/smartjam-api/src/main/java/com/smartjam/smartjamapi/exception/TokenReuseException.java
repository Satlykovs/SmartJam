package com.smartjam.smartjamapi.exception;

import org.springframework.security.core.AuthenticationException;

public class TokenReuseException extends AuthenticationException {
    public TokenReuseException(String msg) {
        super(msg);
    }
}
