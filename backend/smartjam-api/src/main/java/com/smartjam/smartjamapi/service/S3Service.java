package com.smartjam.smartjamapi.service;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.smartjamapi.config.MinioProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@Slf4j
public class S3Service {

    private final MinioProperties minioProperties;
    private final S3Client publicS3Client;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    public S3Service(
            MinioProperties minioProperties,
            S3Client s3Client,
            @Qualifier("publicS3Client") S3Client publicS3Client,
            S3Presigner presigner) {
        this.minioProperties = minioProperties;
        this.s3Client = s3Client;
        this.publicS3Client = publicS3Client;
        this.presigner = presigner;
    }

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
        UUID randomId = UUID.randomUUID();
        return String.format("temp-avatars/%s/%s", userUUID, randomId);
    }

    public String generatePresignedUrlForTeacher(String path) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getReferences())
                .key(getRelativeKey(path))
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedPutObjectRequest.url().toString();
    }

    public String generatePresignedUrlForStudent(String path) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getSubmissions())
                .key(getRelativeKey(path))
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedRequest.url().toString();
    }

    public String generatePresignedUrlForDownload(String path) {
        if (path == null || path.isBlank()) {
            throw new EntityNotFoundException("S3 object path is missing");
        }
        String bucket = determineBucket(path);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(getRelativeKey(path))
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(
                r -> r.getObjectRequest(getObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedGetObjectRequest.url().toString();
    }

    private String getRelativeKey(String path) {
        if (path == null) return null;
        int index = path.indexOf("/");
        return (index != -1) ? path.substring(index + 1) : path;
    }

    private String determineBucket(String path) {
        if (path != null && path.startsWith("submissions/")) {
            return minioProperties.getBuckets().getSubmissions();
        } else if (path != null && path.startsWith("references/")) {
            return minioProperties.getBuckets().getReferences();
        } else if (path != null && path.startsWith("avatars/")) {
            return minioProperties.getBuckets().getAvatars();
        }
        throw new IllegalArgumentException("Unknown S3 path prefix: " + path);
    }

    public String generateUrlForUserAvatar(String key) {
        return publicS3Client
                .utilities()
                .getUrl(builder -> builder.bucket(minioProperties.getBuckets().getAvatars())
                        .key(key))
                .toExternalForm();
    }

    public String generatePresignedUrlForUserAvatar(String path) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getTempAvatars())
                .key(getRelativeKey(path))
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

    public Instant getObjectLastModified(String bucket, String key) {
        try {
            HeadObjectRequest request =
                    HeadObjectRequest.builder().bucket(bucket).key(key).build();
            HeadObjectResponse response = s3Client.headObject(request);
            return response.lastModified();
        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении заголовков S3", e);
        }
    }
}
