package com.smartjam.smartjamanalyzer.service;


import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService
{
    private final MinioClient minioClient;

    public Path downloadAudioFile(String bucketName, String fileKey)
    {
        log.info("Начинаем скачивание файла [{}] из бакета [{}]", fileKey, bucketName);

        try
        {
            String flatFileName = fileKey.replace("/", "_");
            Path tempFilePath = Files.createTempFile("smartjam_", flatFileName);

            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .filename(tempFilePath.toString())
                    .overwrite(true)
                    .build());

            log.info("Файл скачан: {}", tempFilePath.toAbsolutePath());
            return tempFilePath;
        } catch (Exception e)
        {
            log.error("Ошибка при скачивании из MinIO: {}", e.getMessage());

            throw new RuntimeException("Failed to download file", e);
        }
    }
}
