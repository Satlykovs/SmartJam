package com.smartjam.smartjamapi.dto;

import java.time.Instant;
import lombok.Builder;
import org.springframework.http.HttpStatus;

/**
 * DTO representing a standardized error response returned by the API.
 *
 * @param code error classification code
 * @param message human-readable error description
 * @param errorTime timestamp when the error response was created
 */
@Builder
public record ErrorResponseDto(HttpStatus code, String message, Instant errorTime) {}
