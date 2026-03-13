package com.smartjam.smartjamapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenRequest(
        @JsonProperty("refresh_token") String refreshToken) {}
