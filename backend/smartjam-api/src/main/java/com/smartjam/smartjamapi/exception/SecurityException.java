package com.smartjam.smartjamapi.exception;

import org.springframework.security.core.AuthenticationException;

public class SecurityException extends AuthenticationException {
    public SecurityException(String message) {
        super(message);
    }
}
