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

    /**
     * Constructs the S3 object key for a reference file belonging to an assignment.
     *
     * @param connectionId UUID of the connection (first path segment)
     * @param assignmentId UUID of the assignment (second path segment)
     * @return the S3 key in the form `references/{connectionId}/{assignmentId}`
     */
    public String getAssignmentKey(UUID connectionId, UUID assignmentId) {
        return String.format("references/%s/%s", connectionId, assignmentId);
    }

    /**
     * Builds the S3 object key for a submission.
     *
     * The key is formatted as "submissions/{assignmentId}/{submissionId}".
     *
     * @param assignmentId UUID of the assignment
     * @param submissionId UUID of the submission
     * @return the S3 key for the submission (e.g., "submissions/{assignmentId}/{submissionId}")
     */
    public String getSubmissionKey(UUID assignmentId, UUID submissionId) {
        return String.format("submissions/%s/%s", assignmentId, submissionId);
    }

    /**
     * Builds the S3 object key for a user's avatar.
     *
     * @param userUUID the user's UUID
     * @return the object key in the form "avatars/{userUUID}"
     */
    public String getAvatarsKey(UUID userUUID) {
        return String.format("avatars/%s", userUUID);
    }

    /**
     * Builds the S3 object key for a user's temporary avatar.
     *
     * @param userUUID the user's UUID to include in the key
     * @return the object key in the form `temp-avatars/{userUUID}`
     */
    public String getTempAvatarsKey(UUID userUUID) {
        return String.format("temp-avatars/%s", userUUID);
    }

    /**
     * Create a presigned PUT URL for uploading a teacher reference object.
     *
     * @param key the S3 object key; if the key contains a prefix (e.g. "references/..."), only the substring after the first '/' is used
     * @return the presigned PUT URL valid for 10 minutes
     */
    public String generatePresignedUrlForTeacher(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getReferences())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedPutObjectRequest.url().toString();
    }

    /**
     * Generate a presigned PUT URL to upload a student submission object.
     *
     * @param key the S3 object key; if the key contains a prefix (for example, "submissions/abc"),
     *            only the substring after the first '/' is used as the object key stored in the submissions bucket
     * @return the presigned PUT URL targeting the submissions bucket, valid for 10 minutes
     */
    public String generatePresignedUrlForStudent(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getSubmissions())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedRequest.url().toString();
    }

    /**
     * Generate a presigned GET URL for downloading the S3 object identified by the given key.
     *
     * @param key the S3 object key; may be a namespaced key with a prefix such as "submissions/", "references/", or "avatars/"
     * @return a presigned URL string that can be used to download the object; valid for 10 minutes
     * @throws EntityNotFoundException if {@code key} is null or blank
     */
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

    /**
     * Extracts the portion of an S3 object key after the first '/' separator.
     *
     * If {@code key} is null, returns null. If {@code key} contains a '/', returns the substring after the first '/', otherwise returns {@code key} unchanged.
     *
     * @param key the S3 object key which may include a prefix (for example, "prefix/relativeKey")
     * @return the substring after the first '/', the original {@code key} if no '/' is present, or {@code null} if {@code key} is null
     */
    private String getRelativeKey(String key) {
        if (key == null) return null;
        int index = key.indexOf("/");
        return (index != -1) ? key.substring(index + 1) : key;
    }

    /**
     * Selects the target bucket name based on the S3 object key's top-level prefix.
     *
     * @param key the S3 object key expected to start with one of: "submissions/", "references/", or "avatars/"
     * @return the configured bucket name corresponding to the key's prefix
     * @throws IllegalArgumentException if the key is null or does not start with a recognized prefix
     */
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

    /**
     * Builds an external (non-presigned) URL for a user avatar stored in the avatars bucket.
     *
     * @param key the S3 object key for the avatar (relative key within the avatars bucket)
     * @return the external URL string pointing to the avatar object
     */
    public String generateUrlForUserAvatar(String key) {
        return s3Client.utilities()
                .getUrl(builder -> builder.bucket(minioProperties.getBuckets().getAvatars())
                        .key(key))
                .toExternalForm();
    }

    /**
     * Generates a presigned PUT URL for uploading a user's avatar to the temporary avatars bucket.
     *
     * @param key S3 object key or namespaced key for the avatar; if a prefix is present only the relative key is used
     * @return A presigned PUT URL for the temporary avatars bucket that is valid for 10 minutes
     */
    public String generatePresignedUrlForUserAvatar(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioProperties.getBuckets().getTempAvatars())
                .key(getRelativeKey(key))
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.putObjectRequest(putObjectRequest).signatureDuration(Duration.ofMinutes(10)));

        return presignedRequest.url().toString();
    }

    /**
     * Retrieve an object's data as an InputStream from the specified S3 bucket and key.
     *
     * @param bucket the S3 bucket name
     * @param key    the object key within the bucket
     * @return       an InputStream of the object's content
     */
    public InputStream getObjectStream(String bucket, String key) {
        return s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * Uploads the provided byte content to the specified S3 bucket under the given object key.
     *
     * @param bucket      the name of the target S3 bucket
     * @param key         the object key (path) where the content will be stored
     * @param content     the byte array to upload
     * @param contentType the MIME type to assign to the stored object
     */
    public void putObject(String bucket, String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
    }

    /**
     * Delete the object at the specified bucket and key from S3.
     *
     * @param bucket the S3 bucket name containing the object
     * @param key    the object key to delete
     */
    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
