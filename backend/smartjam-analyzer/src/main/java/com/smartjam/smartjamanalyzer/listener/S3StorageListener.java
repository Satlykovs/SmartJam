package com.smartjam.smartjamanalyzer.listener;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.dto.S3EventDto;
import com.smartjam.smartjamanalyzer.service.AudioProcessorService;
import com.smartjam.smartjamanalyzer.service.StorageService;
import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * Kafka listener responsible for processing S3 object creation events. It coordinates the storage and processing
 * pipeline for incoming audio files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageListener {
    private final StorageService storageService;
    private final AudioProcessorService audioProcessorService;

    /**
     * Processes file upload events from the S3 Kafka topic.
     *
     * @param event The parsed S3 event DTO containing file metadata.
     * @param ack The Kafka acknowledgment object for manual offset management.
     * @throws RuntimeException if processing is failed
     */
    @KafkaListener(
            topics = "s3-events",
            groupId = "smartjam-analyzer-group",
            concurrency = "3",
            properties = {"spring.json.value.default.type=com.smartjam.smartjamanalyzer.dto" + ".S3EventDto"})
    public void onFileUploaded(S3EventDto event, Acknowledgment ack) {
        if (event == null || event.records() == null || event.records().isEmpty()) {
            if (ack != null) ack.acknowledge();
            return;
        }
        for (S3EventDto.S3Record s3Record : event.records()) {

            if (s3Record == null) {
                log.warn("Получен null S3 record, пропускаем");
                continue;
            }
            if (s3Record.s3() == null
                    || s3Record.s3().bucket() == null
                    || s3Record.s3().bucket().name() == null
                    || s3Record.s3().object() == null
                    || s3Record.s3().object().key() == null) {
                log.warn("Получен некорректный S3 payload, пропускаем: {}", s3Record);
                continue;
            }

            String bucket = s3Record.s3().bucket().name();
            String fileKey = "UNKNOWN_KEY";

            // NOTE: Добавить нормальный сбор метрик

            try (TempWorkspace workspace = new TempWorkspace()) {

                fileKey = URLDecoder.decode(s3Record.s3().object().key(), StandardCharsets.UTF_8);

                log.info("-------------------------------------------------------");
                log.info("[S3 EVENT] Поймали новое событие: {}", s3Record.eventName());
                log.info("Бакет: {}", bucket);
                log.info("Файл: {}", fileKey);

                StopWatch watch = new StopWatch(fileKey);

                watch.start("Download S3");
                Path localFile = storageService.downloadAudioFile(bucket, fileKey, workspace);
                watch.stop();

                watch.start("FFmpeg convert");
                Path cleanWavFile = audioProcessorService.convertToStandardWav(localFile, workspace);
                watch.stop();

                log.info(
                        "Аудио готово к анализу: {} (Размер: {} байт)",
                        cleanWavFile.getFileName(),
                        Files.size(cleanWavFile));

                watch.start("Math and algos");
                if ("references".equals(bucket)) {
                    log.info("Действие: Обработка ЭТАЛОНА учителя...");
                } else if ("submissions".equals(bucket)) {
                    log.info("Действие: Обработка ПОПЫТКИ ученика...");
                }

                watch.stop();

                log.info("Время выполнения: \n{}", watch.prettyPrint());

                log.info("-------------------------------------------------------");

            } catch (Exception e) {
                log.error("Ошибка пайплайна для файла [{}] из бакета [{}]: {}", fileKey, bucket, e.getMessage(), e);

                throw new RuntimeException("Pipeline processing failed for " + fileKey, e);
            }
        }
        if (ack != null) {
            ack.acknowledge();
        }
    }
}
