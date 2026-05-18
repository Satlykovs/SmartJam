package com.smartjam.smartjamapi.service;

import java.time.Duration;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

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

    public String getAssignmentKey(UUID connectionId, UUID assignmentId) {
        return String.format("references/%s/%s", connectionId, assignmentId);
    }

    public String generatePresignedUrlForTeacher(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getReferences())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedPutObjectRequest.url().toString();
    }

    public String generatePresignedUrlForDownload(String key) {
        if (key == null || key.isBlank()) {
            throw new EntityNotFoundException("S3 object key is missing");
        }
        String bucket = determineBucket(key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(getRelativeKey(key))
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(
                r -> r.getObjectRequest(getObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedGetObjectRequest.url().toString();
    }

    private String determineBucket(String key) {
        if (key != null && key.startsWith("submissions/")) {
            return minioProperties.getBuckets().getSubmissions();
        }
        return minioProperties.getBuckets().getReferences();
    }

    private String getRelativeKey(String key) {
        if (key == null) return null;
        int index = key.indexOf("/");
        return (index != -1) ? key.substring(index + 1) : key;
    }

    public String getSubmissionKey(UUID assignmentId, UUID submissionId) {
        return String.format("submissions/%s/%s", assignmentId, submissionId);
    }

    public String generatePresignedUrlForStudent(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getSubmissions())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedRequest.url().toString();
    }
}
