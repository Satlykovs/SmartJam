package com.smartjam.smartjamapi.dto;

import jakarta.validation.constraints.NotNull;

public record UploadRequest(@NotNull String fileName) {}
