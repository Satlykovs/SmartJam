package com.smartjam.smartjamapi.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadRequest(@NotBlank String fileName) {}
