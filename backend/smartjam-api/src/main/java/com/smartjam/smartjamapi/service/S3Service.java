package com.smartjam.smartjamapi.service;

import java.time.Duration;
import java.util.UUID;

import com.smartjam.smartjamapi.config.MinioProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@Slf4j
@AllArgsConstructor
public class S3Service {

    private final MinioProperties minioProperties;

    private final S3Presigner presigner;

    public String getKey(UUID connectionId, UUID assignmentId) {
        return String.format("references/%s/%s", connectionId, assignmentId);
    }

    public String generatePresignedUrlForTeacher(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getReferences())
                .key(key)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedPutObjectRequest.url().toString();
    }

    public String generatePresignedUrlForDownload(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getReferences())
                .key(key)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(
                r -> r.getObjectRequest(getObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedGetObjectRequest.url().toString();
    }
}
