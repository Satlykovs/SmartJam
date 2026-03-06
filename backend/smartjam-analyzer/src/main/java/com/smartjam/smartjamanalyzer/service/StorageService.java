package com.smartjam.smartjamanalyzer.service;

import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Сервис для взаимодействия с S3-хранилищем (MinIO). */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioClient minioClient;

    /**
     * Скачивает аудиофайл из указанного бакета и сохраняет его во временную директорию ОС. После скачивания файл готов
     * к обработке FFmpeg.
     *
     * @param bucketName Имя бакета (references или submissions)
     * @param fileKey Путь к объекту в S3
     * @param workspace Временное рабочее пространство, которое создает и очищает временные файлы
     * @return Path к временному файлу на диске
     * @throws RuntimeException если скачивание не удалось (например, нет сети или файла)
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
