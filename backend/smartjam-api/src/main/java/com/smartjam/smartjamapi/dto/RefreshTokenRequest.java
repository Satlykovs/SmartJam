package com.smartjam.smartjamapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;


public record RefreshTokenRequest(
        @NotBlank
        @JsonProperty("refresh_token") String refreshToken) {}
