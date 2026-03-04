package com.smartjam.smartjamapi.dto;

public record LoginRequest(
    String email,
    String password
) {
}
