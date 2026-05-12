package com.smartjam.smartjamapi.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@ToString(exclude = {"accessKey", "secretKey"})
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Buckets buckets;

    @Getter
    @Setter
    public static class Buckets {
        private String references;
        private String submissions;
    }
}
