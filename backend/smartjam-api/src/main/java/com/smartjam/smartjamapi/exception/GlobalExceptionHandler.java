package com.smartjam.smartjamapi.exception;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.smartjamapi.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global REST exception handler that converts server-side exceptions into standardized HTTP responses with
 * {@link ErrorResponseDto}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Builds a standardized error response DTO with the provided status, code, and message.
     *
     * @param status HTTP status to return
     * @param message human-readable error message
     * @return response entity containing the error DTO
     */
    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message) {
        var dto = ErrorResponseDto.builder()
                .code(status)
                .message(message)
                .errorTime(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(dto);
    }

    /**
     * Handles authentication failures caused by unknown usernames.
     *
     * @param e thrown exception
     * @return unauthorized response
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameNotFound(UsernameNotFoundException e) {
        log.warn("Authentication failed: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    /**
     * Handles generic Spring Security authentication exceptions.
     *
     * @param e thrown exception
     * @return unauthorized response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(AuthenticationException e) {
        log.warn("Unauthenticated: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }

    /**
     * Handles illegal application state errors.
     *
     * @param e thrown exception
     * @return internal server error response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalStateException(IllegalStateException e) {
        log.error("Illegal state", e);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    /**
     * Handles bean validation errors caused by invalid request payload data.
     *
     * @param e thrown exception
     * @return bad request response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed", e);

        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request data");
    }

    /**
     * Handles invalid method arguments and malformed request data.
     *
     * @param e thrown exception
     * @return bad request response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad request", e);

        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request data");
    }

    /**
     * Handles missing elements requested from collections or services.
     *
     * @param e thrown exception
     * @return not found response
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSuchElement(NoSuchElementException e) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, "Requested resource not found");
    }

    /**
     * Handles JPA entity lookup failures.
     *
     * @param e thrown exception
     * @return not found response
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("Entity not found", e);

        return buildResponse(HttpStatus.NOT_FOUND, "Requested resource not found");
    }

    /**
     * Handles requests to non-existing HTTP resources.
     *
     * @param e thrown exception
     * @return not found response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found");
    }

    /**
     * Fallback handler for all unexpected exceptions.
     *
     * @param e thrown exception
     * @return internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception e) {
        log.error("Unexpected error", e);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
