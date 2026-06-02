package com.smartjam.smartjamapi.record;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record S3WebhookPayload(@JsonProperty("Records") List<S3EventRecord> records) {
    public record S3EventRecord(S3Entity s3) {}

    public record S3Entity(S3Bucket bucket, S3Object object) {}

    public record S3Bucket(String name) {}

    public record S3Object(String key, Long size) {}
}
