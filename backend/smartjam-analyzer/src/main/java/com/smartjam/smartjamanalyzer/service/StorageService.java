package com.smartjam.smartjamanalyzer.service;

import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for interacting with S3-compatible storage (MinIO). */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioClient minioClient;

    /**
     * Downloads an audio file from the specified bucket to a local temporary location.
     *
     * @param bucketName The name of the S3 bucket.
     * @param fileKey The object key in the S3 bucket.
     * @param workspace The workspace that registers the downloaded file for cleanup.
     * @return A {@link Path} to the downloaded file.
     * @throws RuntimeException if the download fails (e.g., network error or file not found).
     */
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
