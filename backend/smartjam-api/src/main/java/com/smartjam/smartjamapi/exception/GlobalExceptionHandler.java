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

// TODO: Это базовый шаблон для обработки ошибок
// TODO: Возможно, некоторые исключения ловятся неправильно
// TODO: Нужно будет потом уточнить маппинг (какое исключение -> какой статус)

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handlerGenericException(Exception e) {
        log.error("Unexpected error: ", e);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerEntityNotFound(EntityNotFoundException e) {
        log.warn("Entity not found: ", e);

        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Requested resource not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthenticated(IllegalArgumentException e) {
        log.warn("Unauthenticated: {}", e.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Unauthenticated");
    }

    @ExceptionHandler(exception = {IllegalStateException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponseDto> handlerBadRequest(Exception e) {
        log.warn("Bad request: ", e);

        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Invalid request data");
    }
}
