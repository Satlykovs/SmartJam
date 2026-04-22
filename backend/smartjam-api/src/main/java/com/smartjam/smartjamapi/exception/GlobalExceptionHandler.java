package com.smartjam.smartjamapi.exception;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.api.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// TODO: обработать исключения кастомные

/**
 * Global REST exception handler that converts server-side exceptions into standardized HTTP responses with
 * {@link ErrorResponse}.
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
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        var dto = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);

        return ResponseEntity.status(status).body(dto);
    }

    /**
     * Handles authentication failures caused by unknown usernames.
     *
     * @param e thrown exception
     * @return unauthorized response
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException e) {
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
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
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
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
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
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
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
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
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
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException e) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    /**
     * Handles JPA entity lookup failures.
     *
     * @param e thrown exception
     * @return not found response
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
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
    public ResponseEntity<ErrorResponse> handleResourceNotFound(NoResourceFoundException e) {
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
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error", e);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    // INFO: это вообще не работает, не знаю как обрабатывать отдельно каждую аннотацию валидации
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {

        String detailedErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> {
                    if (error instanceof FieldError) {
                        return "Field '" + ((FieldError) error).getField() + "': " + error.getDefaultMessage();
                    } else {
                        return error.getDefaultMessage();
                    }
                })
                .collect(Collectors.joining("; "));

        if (detailedErrors.isEmpty()) {
            detailedErrors = "Invalid request data.";
        }

        log.warn("Method validation failed: {}", detailedErrors);

        return buildResponse(HttpStatus.BAD_REQUEST, detailedErrors);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException e) {
        log.warn("Registration conflict: {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ConnectionAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleConnectionAlreadyActiveException(ConnectionAlreadyActiveException e) {
        log.warn("Connection conflict: {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CannotJoinSelfException.class)
    public ResponseEntity<ErrorResponse> handleSelfJoinNotAllowed(CannotJoinSelfException e) {
        log.warn("Self join attempt: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

}
