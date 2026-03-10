package com.smartjam.smartjamapi.dto;

import com.smartjam.smartjamapi.enums.AvailabilityStatus;

public record AuthResponse(
        String message,
        AvailabilityStatus status,
        String refresh_token,
        String access_token
) {
}
