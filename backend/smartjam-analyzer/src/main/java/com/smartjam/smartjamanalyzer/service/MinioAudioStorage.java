package com.smartjam.smartjamanalyzer.service;

import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** MinIO-based implementation of {@link AudioStorage}. */
@Slf4j
@Service
@RequiredArgsConstructor
class MinioAudioStorage implements AudioStorage {
    private final MinioClient minioClient;

    @Override
    public Path downloadAudioFile(String bucketName, String fileKey, TempWorkspace workspace) {
        log.info("Начинаем скачивание файла [{}] из бакета [{}]", fileKey, bucketName);

        try {
            String flatFileName = fileKey.replace("/", "_");
            Path tempFilePath = workspace.createTempFile("smartjam_", flatFileName);

            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .filename(tempFilePath.toString())
                    .overwrite(true)
                    .build());

            log.info("Файл скачан: {}", tempFilePath.toAbsolutePath());
            return tempFilePath;
        } catch (Exception e) {
            log.error("Ошибка при скачивании из MinIO: {}", e.getMessage());

            throw new RuntimeException("Failed to download file", e);
        }
    }
}
