package com.smartjam.smartjamapi.exception;

import org.springframework.security.core.AuthenticationException;

/** Exception thrown when an authentication token has expired. */
public class TokenExpiredException extends AuthenticationException {

    /**
     * Creates a new token expiration exception with the provided message.
     *
     * @param message exception description
     */
    public TokenExpiredException(String message) {
        super(message);
    }
}
