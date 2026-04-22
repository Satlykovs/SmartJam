package com.smartjam.smartjamapi.exception;

public class ConnectionAlreadyActiveException extends RuntimeException {
    public ConnectionAlreadyActiveException(String message) {
        super(message);
    }
}
