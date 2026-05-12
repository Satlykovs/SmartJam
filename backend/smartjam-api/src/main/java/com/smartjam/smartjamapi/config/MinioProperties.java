package com.smartjam.smartjamapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Buckets buckets;

    @Data
    public static class Buckets {
        private String references;
        private String submissions;
    }
}
