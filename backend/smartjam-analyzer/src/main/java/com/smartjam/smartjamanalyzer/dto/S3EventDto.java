package com.smartjam.smartjamanalyzer.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/** Data Transfer Object for S3 Event Notifications. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record S3EventDto(@JsonProperty("Records") List<S3Record> records) {

    /** Record detail. */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Record(
            @JsonProperty("eventName") String eventName,
            @JsonProperty("s3") S3Data s3) {}

    /** S3 specific data. */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Data(
            @JsonProperty("bucket") Bucket bucket,
            @JsonProperty("object") S3Object object) {}

    /** Bucket metadata. */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(@JsonProperty("name") String name) {}

    /** Object metadata. */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Object(
            @JsonProperty("key") String key,
            @JsonProperty("size") Long size) {}
}
