package com.smartjam.smartjamanalyzer.infrastructure.storage;

import jakarta.validation.constraints.NotBlank;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for MinIO connection.
 *
 * @param url The endpoint url of the MinIO server
 * @param accessKey The access key for authentication
 * @param secretKey The secret key for authentication
 */
@ConfigurationProperties(prefix = "minio")
@Validated
record MinioProperties(
        @NotBlank String url,
        @NotBlank String accessKey,
        @NotBlank String secretKey) {}

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
