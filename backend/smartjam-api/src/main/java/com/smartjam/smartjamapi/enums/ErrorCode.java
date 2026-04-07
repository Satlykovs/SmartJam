package com.smartjam.smartjamapi.enums;

/** Enumeration of application-specific error codes used in API error responses. */
public enum ErrorCode {

    /** Unexpected internal server error. */
    INTERNAL_SERVER_ERROR,

    /** Requested resource was not found. */
    NOT_FOUND,

    /** Request payload or parameters are invalid. */
    BAD_REQUEST,

    /** Authentication is required or has failed. */
    UNAUTHORIZED
}
