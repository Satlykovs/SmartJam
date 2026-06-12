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

import com.smartjam.common.dto.s3.S3Event;
import com.smartjam.smartjamapi.config.MinioProperties;
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
    private static final int MAX_INPUT_WIDTH = 4000;
    private static final int MAX_INPUT_HEIGHT = 4000;

    private final MinioProperties minioProperties;
    private final UserRepository repository;
    private final S3Service s3Service;

    public void validateUserAvatar(S3Event payload) {
        String tempBucket = minioProperties.getBuckets().getTempAvatars();
        String targetBucket = minioProperties.getBuckets().getAvatars();

        for (S3Event.S3Record record : payload.records()) {
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

                String format = validateAndGetFormat(new ByteArrayInputStream(fileBytes));
                if (!isAllowed(format)) {
                    log.warn(
                            "Файл {} имеет недопустимый формат или превышает лимиты разрешения: {}", objectKey, format);
                    s3Service.deleteObject(tempBucket, objectKey);
                    continue;
                }

                String[] keyParts = objectKey.split("/");
                if (keyParts.length < 2) {
                    log.error("Неверный формат ключа в temp бакете: {}", objectKey);
                    s3Service.deleteObject(tempBucket, objectKey);
                    continue;
                }

                UUID userId = UUID.fromString(keyParts[0]);
                String targetKey = userId.toString();

                ByteArrayOutputStream os = new ByteArrayOutputStream();

                log.info("Мы зашли, но что-то не так");
                Thumbnails.of(new ByteArrayInputStream(fileBytes))
                        .size(400, 400)
                        .crop(Positions.CENTER)
                        .outputFormat(format)
                        .outputQuality(0.85)
                        .toOutputStream(os);
                log.info("Мы вышли, а не, всё ок");

                byte[] result = os.toByteArray();
                String contentType = "image/" + format;
                s3Service.putObject(targetBucket, targetKey, result, contentType);

                try {
                    String newAvatarUrl = s3Service.generateUrlForUserAvatar(targetKey);
                    int updatedRows = repository.updateAvatarUrl(userId, newAvatarUrl);

                    if (updatedRows == 0) {
                        throw new IllegalStateException("User not found for avatar key " + objectKey);
                    }

                    s3Service.deleteObject(tempBucket, objectKey);
                } catch (Exception e) {
                    s3Service.deleteObject(targetBucket, targetKey);
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

    private String validateAndGetFormat(InputStream is) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);

                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);

                    if (width > MAX_INPUT_WIDTH || height > MAX_INPUT_HEIGHT) {
                        log.error("Bomb has not been planted! Разрешение файла: {}x{}", width, height);
                        return null;
                    }

                    return reader.getFormatName().toLowerCase();
                } finally {
                    reader.dispose();
                }
            }
        }
        return null;
    }

    private boolean isAllowed(String format) {
        if (format == null) return false;
        MinioProperties.FormatAvatar formatAvatar = minioProperties.getFormatAvatar();
        return format.equals(formatAvatar.getJpeg())
                || format.equals(formatAvatar.getJpg())
                || format.equals(formatAvatar.getPng());
    }
}
