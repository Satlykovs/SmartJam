package com.smartjam.smartjamapi.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.smartjam.smartjamapi.config.MinioProperties;
import com.smartjam.smartjamapi.record.S3WebhookPayload;
import com.smartjam.smartjamapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3WebhookService {

    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;

    private final MinioProperties minioProperties;
    private final UserRepository repository;
    private final S3Service s3Service;

    /**
     * Processes S3 webhook records for user avatar uploads by validating size and format,
     * transforming and storing the final avatar in the target bucket, updating the corresponding user
     * record with the avatar URL, and removing temporary objects.
     *
     * Invalid or oversized objects are deleted from the temporary bucket; if associating the avatar
     * with a user fails after upload, the uploaded avatar is removed from the target bucket.
     *
     * @param payload the S3 webhook payload containing event records for temporary avatar objects
     */
    public void validateUserAvatar(S3WebhookPayload payload) {
        String tempBucket = minioProperties.getBuckets().getTempAvatars();
        String targetBucket = minioProperties.getBuckets().getAvatars();

        for (S3WebhookPayload.S3EventRecord record : payload.records()) {
            String rawKey = record.s3().object().key();
            String objectKey = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);

            Long objectSize = record.s3().object().size();
            if (objectSize == null || objectSize > MAX_AVATAR_BYTES) {
                log.warn("Avatar {} exceeds max size: {}", objectKey, objectSize);
                s3Service.deleteObject(tempBucket, objectKey);
                continue;
            }

            try (InputStream is = s3Service.getObjectStream(tempBucket, objectKey)) {
                byte[] fileBytes = is.readAllBytes();
                String format = getFormat(new ByteArrayInputStream(fileBytes));
                if (format == null || !isAllowed(format)) {
                    log.warn("Файл {} имеет недопустимый формат: {}", objectKey, format);
                    s3Service.deleteObject(tempBucket, objectKey);
                    continue;
                }

                ByteArrayOutputStream os = new ByteArrayOutputStream();

                Thumbnails.of(new ByteArrayInputStream(fileBytes))
                        .size(400, 400)
                        .crop(Positions.CENTER)
                        .outputFormat(format)
                        .outputQuality(0.85)
                        .toOutputStream(os);

                byte[] result = os.toByteArray();
                String contentType = "image/" + format;
                s3Service.putObject(targetBucket, objectKey, result, contentType);
                try {
                    UUID userId = UUID.fromString(objectKey);
                    var user = repository
                            .findById(userId)
                            .orElseThrow(() -> new IllegalStateException("User not found for avatar key " + objectKey));
                    user.setAvatarUrl(s3Service.generateUrlForUserAvatar(objectKey));
                    repository.save(user);
                    s3Service.deleteObject(tempBucket, objectKey);
                } catch (Exception e) {
                    s3Service.deleteObject(targetBucket, objectKey);
                    throw e;
                }

                log.info("Аватар {} успешно обработан и перемещен", objectKey);
                log.info("Файл {} имеет формат: {}", objectKey, format);

            } catch (Exception e) {
                log.error("Ошибка при обработке аватара {}: {}", objectKey, e.getMessage());
                try {
                    s3Service.deleteObject(tempBucket, objectKey);
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Determines the image format name from the provided input stream.
     *
     * @param is an input stream positioned at the start of the image data
     * @return the image format name in lowercase (e.g., "png", "jpeg"), or `null` if the format cannot be determined
     * @throws IOException if an I/O error occurs while reading the stream
     */
    private String getFormat(InputStream is) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    return reader.getFormatName().toLowerCase();
                } finally {
                    reader.dispose();
                }
            }
        }
        return null;
    }

    /**
     * Determines whether the provided image format is permitted for user avatars.
     *
     * @param format the image format name to check; comparison is exact (case-sensitive) against configured values
     * @return {@code true} if the format matches the configured JPEG, JPG, or PNG avatar formats; {@code false} otherwise
     */
    private boolean isAllowed(String format) {
        MinioProperties.FormatAvatar formatAvatar = minioProperties.getFormatAvatar();
        return format.equals(formatAvatar.getJpeg())
                || format.equals(formatAvatar.getJpg())
                || format.equals(formatAvatar.getPng());
    }
}
