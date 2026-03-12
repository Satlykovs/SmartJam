package com.smartjam.smartjamapi.exception;

import java.time.LocalDateTime;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.smartjamapi.dto.ErrorResponseDto;
import com.smartjam.smartjamapi.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handlerGenericException(Exception e) {
        log.error("Unexpected error: ", e);

        var errorDto =
                new ErrorResponseDto(ErrorCode.INTERNAL_SERVER_ERROR, "Internal server error", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerEntityNotFound(EntityNotFoundException e) {
        log.warn("Entity not found: ", e);

        var errorDto =
                new ErrorResponseDto(ErrorCode.NON_FOUND_PAGE, "Requested resource not found", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("No handler found for request: {}", e.getRequestURL());

        var errorDto =
                new ErrorResponseDto(ErrorCode.NON_FOUND_PAGE, "Requested resource not found", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthenticated(ResponseStatusException e) {
        log.warn("Unauthenticated: {}", e.getMessage());

        var errorDto = new ErrorResponseDto(ErrorCode.UNAUTHORIZED, e.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(e.getStatusCode()).body(errorDto);
    }

    @ExceptionHandler(
            exception = {
                IllegalArgumentException.class,
                IllegalStateException.class,
                MethodArgumentNotValidException.class
            })
    public ResponseEntity<ErrorResponseDto> handlerBadRequest(Exception e) {
        log.warn("Bad request: ", e);

        var errorDto = new ErrorResponseDto(ErrorCode.BAD_REQUEST, "Invalid request data", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }
}
