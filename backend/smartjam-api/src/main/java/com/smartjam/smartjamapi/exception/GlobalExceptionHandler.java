package com.smartjam.smartjamapi.exception;

import java.time.LocalDateTime;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.smartjamapi.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handlerGenericException(Exception e) {
        log.error("Handler exception", e);

        var errorDto = new ErrorResponseDto("INTERNAL_SERVER_ERROR", e.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerEntityNotFound(EntityNotFoundException e) {
        log.error("Handler handlerEntityNotFound", e);

        var errorDto = new ErrorResponseDto("Not Found page", e.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    @ExceptionHandler(
            exception = {
                IllegalArgumentException.class,
                IllegalStateException.class,
                MethodArgumentNotValidException.class
            })
    public ResponseEntity<ErrorResponseDto> handlerBadRequest(Exception e) {
        log.error("Handler handlerBadRequest", e);

        var errorDto = new ErrorResponseDto("Bad request", e.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }
}
