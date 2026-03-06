package com.smartjam.smartjamanalyzer.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration properties for MinIO connection. */
@ConfigurationProperties(prefix = "minio")
record MinioProperties(String url, String accessKey, String secretKey) {}

/**
 * Configuration class for the MinIO client. Enables automatic configuration properties binding for S3/MinIO
 * connectivity.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    /**
     * Builds and provides the {@link MinioClient} instance.
     *
     * @param props MinIO configuration properties.
     * @return a configured {@link MinioClient}.
     */
    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.url())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }
}
