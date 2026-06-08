package com.smartjam.smartjamapi.record;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public record S3WebhookPayload(
        @JsonProperty("Records") @NotEmpty List<@Valid S3EventRecord> records) {
    public record S3EventRecord(@NotNull @Valid S3Entity s3) {}

    public record S3Entity(
            @NotNull @Valid S3Bucket bucket, @NotNull @Valid S3Object object) {}

    public record S3Bucket(@NotBlank String name) {}

    public record S3Object(@NotBlank String key, Long size) {}
}
