package com.smartjam.smartjamapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartjam.smartjamapi.enums.AvailabilityStatus;

public record AuthResponse(
        String message,
        AvailabilityStatus status,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("access_token") String accessToken) {}
