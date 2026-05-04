package com.smartjam.smartjamapi.exception;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.api.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
     * Handles cases where a refresh token has already been used (potential theft).
     *
     * @param e thrown exception
     * @return unauthorized response with reuse message
     */
    @ExceptionHandler(TokenReuseException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuse(TokenReuseException e) {
        log.error("CRITICAL SECURITY ISSUE: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /**
     * Handles expired refresh tokens.
     *
     * @param e thrown exception
     * @return unauthorized response with expiration message
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException e) {
        log.warn("Token expired: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /**
     * Handles cases where a refresh token is not found in the database.
     *
     * @param e thrown exception
     * @return unauthorized response with not found message
     */
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTokenNotFound(TokenNotFoundException e) {
        log.warn("Token not found: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
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
        String detailedErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> "Field '" + fe.getField() + "': " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (detailedErrors.isEmpty()) {
            detailedErrors = "Invalid request data";
        }

        log.warn("DTO validation failed: {}", detailedErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, detailedErrors);
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
        log.warn("URL not found: {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found");
    }

    /**
     * Handles cases where an authenticated user attempts to access a restricted resource.
     *
     * @return forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied() {
        log.warn(
                "Access denied for user: {}",
                SecurityContextHolder.getContext().getAuthentication() != null
                        ? SecurityContextHolder.getContext().getAuthentication().getName()
                        : "anonymous");

        return buildResponse(HttpStatus.FORBIDDEN, "You don't have permission to perform this action");
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

    /**
     * Handles validation errors from controller method parameters.
     *
     * @param e thrown exception
     * @return bad request response
     */
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

    /**
     * Handles conflicts when trying to register a user that already exists.
     *
     * @param e thrown exception
     * @return conflict response
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException e) {
        log.warn("Registration conflict: {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Handles cases where a connection is already established and active.
     *
     * @param e thrown exception
     * @return conflict response
     */
    @ExceptionHandler(ConnectionAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleConnectionAlreadyActiveException(ConnectionAlreadyActiveException e) {
        log.warn("Connection conflict: {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Handles business logic violations where a user tries to join their own entity.
     *
     * @param e thrown exception
     * @return bad request response
     */
    @ExceptionHandler(CannotJoinSelfException.class)
    public ResponseEntity<ErrorResponse> handleSelfJoinNotAllowed(CannotJoinSelfException e) {
        log.warn("Self join attempt: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Handles cases where a requested element is missing in the data source.
     *
     * @param e thrown exception
     * @return not found response
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException e) {
        log.warn("Data not found: {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Requested element not found");
    }

    /**
     * Handles login failures due to wrong password or email.
     *
     * @param e thrown exception
     * @return unauthorized response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        log.warn("Login failed: {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
