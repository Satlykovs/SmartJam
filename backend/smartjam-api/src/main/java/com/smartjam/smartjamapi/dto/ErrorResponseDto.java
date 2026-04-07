package com.smartjam.smartjamapi.dto;

import java.time.LocalDateTime;

import com.smartjam.smartjamapi.enums.ErrorCode;
import lombok.Builder;

/**
 * DTO representing a standardized error response returned by the API.
 *
 * @param code error classification code
 * @param message human-readable error description
 * @param errorTime timestamp when the error response was created
 */
@Builder
public record ErrorResponseDto(ErrorCode code, String message, LocalDateTime errorTime) {}
