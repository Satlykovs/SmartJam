package com.smartjam.smartjamapi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@ToString(exclude = {"accessKey", "secretKey"})
public class MinioProperties {
    @NotBlank
    private String endpoint;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @Valid
    private Buckets buckets;

    @Getter
    @Setter
    public static class Buckets {
        @NotBlank
        private String references;

        @NotBlank
        private String submissions;
    }
}
