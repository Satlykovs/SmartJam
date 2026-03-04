package com.smartjam.smartjamapi.dto;

public record RegisterRequest(
        String username,
        String email,
        String password
) {
}
