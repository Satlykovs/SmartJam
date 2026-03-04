package com.smartjam.smartjamapi.dto;

import com.smartjam.smartjamapi.enums.AvailabilityStatus;

public record AuthResponse(String message, AvailabilityStatus status) {}
