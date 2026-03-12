package com.smartjam.smartjamapi.dto;

import java.time.LocalDateTime;

import com.smartjam.smartjamapi.enums.ErrorCode;

public record ErrorResponseDto(ErrorCode code, String message, LocalDateTime errorTime) {}
