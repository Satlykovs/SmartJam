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
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageListener {
    private final StorageService storageService;
    private final AudioProcessorService audioProcessorService;

    @KafkaListener(
            topics = "s3-events",
            groupId = "smartjam-analyzer-group",
            concurrency = "3",
            properties = {"spring.json.value.default.type=com.smartjam.smartjamanalyzer.dto" + ".S3EventDto"})
    public void onFileUploaded(S3EventDto event) {
        if (event.records() == null || event.records().isEmpty()) {
            return;
        }
        for (S3EventDto.S3Record s3Record : event.records()) {
            StopWatch watch = new StopWatch(s3Record.s3().object().key());

            try (TempWorkspace workspace = new TempWorkspace()) {

                var s3Data = s3Record.s3();
                String bucket = s3Data.bucket().name();

                String fileKey = URLDecoder.decode(s3Data.object().key(), StandardCharsets.UTF_8);

                log.info("-------------------------------------------------------");
                log.info("[S3 EVENT] Поймали новое событие: {}", s3Record.eventName());
                log.info("Бакет: {}", bucket);
                log.info("Файл: {}", fileKey);

                watch.start("Download S3");

                Path localFile = workspace.register(storageService.downloadAudioFile(bucket, fileKey));

                watch.stop();

                watch.start("FFmpeg convert");

                Path cleanWavFile = workspace.register(audioProcessorService.convertToStandardWav(localFile));

                watch.stop();

                log.info(
                        "Аудио готово к анализу: {} (Размер: {} байт)",
                        cleanWavFile.getFileName(),
                        Files.size(cleanWavFile));

                watch.start("Math and algos");
                if ("references".equals(bucket)) {
                    log.info("🛠 Действие: Обработка ЭТАЛОНА учителя...");
                } else if ("submissions".equals(bucket)) {
                    log.info("🛠 Действие: Обработка ПОПЫТКИ ученика...");
                }

                watch.stop();

                log.info("Время выполнения: \n{}", watch.prettyPrint());

                log.info("-------------------------------------------------------");

            } catch (Exception e) {
                log.error("Ошибка пайплайна: {}", e.getMessage());
            }
        }
    }
}
