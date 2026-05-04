package com.smartjam.smartjamapi.exception;

import org.springframework.security.core.AuthenticationException;

/** Exception thrown when an expected authentication token cannot be found. */
public class TokenNotFoundException extends AuthenticationException {

    /**
     * Creates a new token not found exception with the provided message.
     *
     * @param message exception description
     */
    public TokenNotFoundException(String message) {
        super(message);
    }
}
