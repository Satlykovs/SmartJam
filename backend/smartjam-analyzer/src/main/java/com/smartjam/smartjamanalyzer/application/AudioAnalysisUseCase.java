package com.smartjam.smartjamanalyzer.application;

import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.domain.port.AudioConverter;
import com.smartjam.smartjamanalyzer.domain.port.AudioStorage;
import com.smartjam.smartjamanalyzer.domain.port.Workspace;
import com.smartjam.smartjamanalyzer.domain.port.WorkspaceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioAnalysisUseCase {

    private final AudioStorage audioStorage;
    private final AudioConverter audioConverter;
    private final WorkspaceFactory workspaceFactory;

    public void execute(String bucket, String fileKey) {
        try (Workspace workspace = workspaceFactory.create()) {

            //NOTE: Добавить обработку(проверку типа) входящего файла

            // NOTE: Добавить нормальный сбор метрик
            StopWatch watch = new StopWatch(fileKey);

            log.info("=== Начало обработки файла: {} из бакета {} ===", fileKey, bucket);

            watch.start("Download S3");
            Path localFile = audioStorage.downloadAudioFile(bucket, fileKey, workspace);
            watch.stop();

            watch.start("FFmpeg convert");
            Path cleanWavFile = audioConverter.convertToStandardWav(localFile, workspace);
            watch.stop();

            watch.start("Business Logic (Math)");
            if ("references".equals(bucket)) {
                log.info("Действие: Обработка ЭТАЛОНА учителя...");
            } else if ("submissions".equals(bucket)) {
                log.info("Действие: Обработка ПОПЫТКИ ученика...");
            }
            watch.stop();

            log.info("Результаты обработки {}: \n{}", fileKey, watch.prettyPrint());

        } catch (Exception e) {
            log.error("Ошибка в UseCase для файла {}: {}", fileKey, e.getMessage());
            throw new RuntimeException("Business logic failed", e);
        }
    }
}
