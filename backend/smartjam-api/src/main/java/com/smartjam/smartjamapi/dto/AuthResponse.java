package com.smartjam.smartjamapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("access_token") String accessToken) {}
