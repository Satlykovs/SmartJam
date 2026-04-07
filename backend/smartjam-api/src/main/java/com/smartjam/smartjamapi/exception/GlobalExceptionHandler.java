package com.smartjam.smartjamapi.exception;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.smartjamapi.dto.ErrorResponseDto;
import com.smartjam.smartjamapi.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, ErrorCode errorCode, String message) {
        var dto = ErrorResponseDto.builder()
                .code(errorCode)
                .message(message)
                .errorTime(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(dto);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameNotFound(UsernameNotFoundException e) {
        log.warn("Authentication failed: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(AuthenticationException e) {
        log.warn("Unauthenticated: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Unauthenticated");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalStateException(IllegalStateException e) {
        log.error("Illegal state", e);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        String validationMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": "
                        + (fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"))
                .distinct()
                .collect(Collectors.joining("; "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                validationMessage.isBlank() ? "Invalid request data" : validationMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad request", e);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                e.getMessage() != null ? e.getMessage() : "Invalid request data");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSuchElement(NoSuchElementException e) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Requested resource not found");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("Entity not found", e);

        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Requested resource not found");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "The requested resource was not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception e) {
        log.error("Unexpected error", e);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR, "Internal server error");
    }
}
