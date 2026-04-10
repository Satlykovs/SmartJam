package com.smartjam.common.dto.s3;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Data Transfer Object for S3 Event Notifications.
 *
 * @param records List of S3 event records
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record S3EventDto(@JsonProperty("Records") List<S3Record> records) {

    /**
     * Record detail.
     *
     * @param eventName The type of event (e.g., ObjectCreated:Put)
     * @param s3 The container for S3 bucket and object data
     */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Record(
            @JsonProperty("eventName") String eventName,
            @JsonProperty("s3") S3Data s3) {}

    /**
     * S3 specific data.
     *
     * @param bucket The bucket metadata
     * @param object The object metadata
     */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Data(
            @JsonProperty("bucket") Bucket bucket,
            @JsonProperty("object") S3Object object) {}

    /**
     * Bucket metadata.
     *
     * @param name The name of the bucket
     */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(@JsonProperty("name") String name) {}

    /**
     * Object metadata.
     *
     * @param key The storage key of the object
     * @param size The size of the object in bytes
     */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Object(
            @JsonProperty("key") String key,
            @JsonProperty("size") Long size) {}
}
