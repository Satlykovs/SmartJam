package com.smartjam.smartjamapi.dto;

import jakarta.validation.constraints.NotNull;

public record UploadUrlResponse(@NotNull String uploadUrl) {}
