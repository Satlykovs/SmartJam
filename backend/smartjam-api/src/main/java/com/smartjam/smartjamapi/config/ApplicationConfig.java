package com.smartjam.smartjamapi.config;

import java.net.URI;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MinioProperties.class)
public class ApplicationConfig {
    /**
     * Configures a PasswordEncoder that hashes passwords using BCrypt.
     *
     * @return a PasswordEncoder implementation that uses BCrypt for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates and configures an AWS S3Client tailored for MinIO usage.
     *
     * @param minioProperties configuration providing the endpoint (or public endpoint) and static credentials used to configure the client
     * @return an S3Client configured with the endpoint override from {@code minioProperties}, fixed {@code Region.US_EAST_1}, static access key/secret, and S3 path-style access enabled
     */
    @Bean
    public S3Client s3Client(MinioProperties minioProperties) {
        return S3Client.builder()
                .endpointOverride(URI.create(minioProperties.getPublicEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(minioProperties.getAccessKey(), minioProperties.getSecretKey())))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /**
     * Create and configure an S3Presigner for MinIO using application MinioProperties.
     *
     * <p>Chooses the presigner endpoint by using `minioProperties.getPublicEndpoint()` when it is
     * non-null and not blank; otherwise `minioProperties.getEndpoint()` is used. The presigner is
     * configured with Region.US_EAST_1, path-style access enabled, and static credentials derived from
     * `minioProperties.getAccessKey()` and `minioProperties.getSecretKey()`.
     *
     * @param minioProperties configuration properties containing endpoint and credential values
     * @return an S3Presigner configured with the selected endpoint, Region.US_EAST_1, path-style access enabled, and static credentials
     */
    @Bean
    public S3Presigner s3Presigner(MinioProperties minioProperties) {

        String effectiveEndpoint = (minioProperties.getPublicEndpoint() != null
                        && !minioProperties.getPublicEndpoint().isBlank())
                ? minioProperties.getPublicEndpoint()
                : minioProperties.getEndpoint();

        return S3Presigner.builder()
                .endpointOverride(URI.create(effectiveEndpoint))
                .region(Region.US_EAST_1)
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(minioProperties.getAccessKey(), minioProperties.getSecretKey())))
                .build();
    }
}
