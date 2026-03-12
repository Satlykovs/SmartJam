package com.smartjam.smartjamapi.dto;

import com.smartjam.smartjamapi.enums.ErrorCode;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        ErrorCode code,
        String message,
        LocalDateTime errorTime
) {
}
