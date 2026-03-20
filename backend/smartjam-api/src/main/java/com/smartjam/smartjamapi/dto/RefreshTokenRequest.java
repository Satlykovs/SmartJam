package com.smartjam.smartjamapi.dto;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenRequest(
        @NotBlank @JsonProperty("refresh_token") String refreshToken) {}
