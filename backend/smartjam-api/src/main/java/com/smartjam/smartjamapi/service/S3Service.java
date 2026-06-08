package com.smartjam.smartjamapi.service;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.smartjamapi.config.MinioProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
    private final S3Client s3Client;

    public String getAssignmentKey(UUID connectionId, UUID assignmentId) {
        return String.format("references/%s/%s", connectionId, assignmentId);
    }

    public String getSubmissionKey(UUID assignmentId, UUID submissionId) {
        return String.format("submissions/%s/%s", assignmentId, submissionId);
    }

    public String getAvatarsKey(UUID userUUID) {
        return String.format("avatars/%s", userUUID);
    }

    public String getTempAvatarsKey(UUID userUUID) {
        return String.format("temp-avatars/%s", userUUID);
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

    public String generatePresignedUrlForStudent(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getSubmissions())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedRequest.url().toString();
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

    private String getRelativeKey(String key) {
        if (key == null) return null;
        int index = key.indexOf("/");
        return (index != -1) ? key.substring(index + 1) : key;
    }

    private String determineBucket(String key) {
        if (key != null && key.startsWith("submissions/")) {
            return minioProperties.getBuckets().getSubmissions();
        } else if (key != null && key.startsWith("references/")) {
            return minioProperties.getBuckets().getReferences();
        } else if (key != null && key.startsWith("avatars/")) {
            return minioProperties.getBuckets().getAvatars();
        }
        throw new IllegalArgumentException("Unknown S3 key prefix: " + key);
    }

    public String generatePresignedUrlForUserAvatar(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getTempAvatars())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedRequest.url().toString();
    }

    public InputStream getObjectStream(String bucket, String key) {
        return s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void putObject(String bucket, String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
    }

    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
