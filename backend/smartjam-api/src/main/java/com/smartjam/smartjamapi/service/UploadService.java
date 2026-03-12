package com.smartjam.smartjamapi.service;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import jakarta.annotation.PostConstruct;

import com.smartjam.smartjamapi.dto.UploadUrlResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@Slf4j
public class UploadService {

    private static final String BUCKET_NAME = "music";
    private static final String S3_ENDPOINT = "http://localhost:9000"; // пока захардкодил ссылку

    @PostConstruct
    public void init() {
        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(S3_ENDPOINT))
                .region(Region.AP_EAST_1)
                .serviceConfiguration(b -> b.pathStyleAccessEnabled(true))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create("minioadmin", "minioadmin")))
                .build()) {
            log.info("Trying to connect to MinIO at {}", s3Client);
            if (s3Client.listBuckets().buckets().stream()
                    .noneMatch(b -> b.name().equals(BUCKET_NAME))) {
                s3Client.createBucket(b -> b.bucket(BUCKET_NAME));
                log.info("Bucket '{}' created successfully", BUCKET_NAME);
            } else {
                log.info("Bucket '{}' already exists", BUCKET_NAME);
            }
        }
    }

    public UploadUrlResponse generateUploadUrl(String fileName) {
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(S3_ENDPOINT))
                .region(Region.US_EAST_1)
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create("minioadmin", "minioadmin")))
                .build()) {

            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder().bucket(BUCKET_NAME).key(fileName).build();

            PresignedPutObjectRequest presignedGetObjectRequest = presigner.presignPutObject(
                    r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

            URL presignedUtl = presignedGetObjectRequest.url();
            return new UploadUrlResponse(presignedUtl.toString());
        }
    }
}
