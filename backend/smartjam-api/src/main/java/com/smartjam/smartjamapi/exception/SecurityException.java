package com.smartjam.smartjamapi.exception;

import org.springframework.security.core.AuthenticationException;

/** Custom authentication exception for security-related failures. */
public class SecurityException extends AuthenticationException {

    /**
     * Creates a new security exception with the provided message.
     *
     * @param message exception description
     */
    public SecurityException(String message) {
        super(message);
    }
}
