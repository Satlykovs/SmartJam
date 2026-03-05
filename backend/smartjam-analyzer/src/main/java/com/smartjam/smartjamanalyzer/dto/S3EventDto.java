package com.smartjam.smartjamanalyzer.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record S3EventDto(@JsonProperty("Records") List<S3Record> records) {

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Record(
            @JsonProperty("eventName") String eventName,
            @JsonProperty("s3") S3Data s3) {}

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Data(
            @JsonProperty("bucket") Bucket bucket,
            @JsonProperty("object") S3Object object) {}

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(@JsonProperty("name") String name) {}

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Object(
            @JsonProperty("key") String key,
            @JsonProperty("size") Long size) {}
}
