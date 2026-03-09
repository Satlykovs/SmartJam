package com.smartjam.smartjamanalyzer.api.kafka;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.smartjam.smartjamanalyzer.api.kafka.dto.S3EventDto;
import com.smartjam.smartjamanalyzer.application.AudioAnalysisUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka listener responsible for processing S3 object creation events. It coordinates the storage and processing
 * pipeline for incoming audio files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageListener {
    private final AudioAnalysisUseCase analysisUseCase;

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
            properties = {
                "spring.json.value.default.type=com.smartjam.smartjamanalyzer.api" + ".kafka.dto" + ".S3EventDto"
            })
    public void onFileUploaded(S3EventDto event, Acknowledgment ack) {
        if (event == null || event.records() == null || event.records().isEmpty()) {
            if (ack != null) ack.acknowledge();
            return;
        }

        for (S3EventDto.S3Record s3Record : event.records()) {

            try {

                if (!isVaild(s3Record)) continue;

                String bucket = s3Record.s3().bucket().name();
                String fileKey = URLDecoder.decode(s3Record.s3().object().key(), StandardCharsets.UTF_8);

                analysisUseCase.execute(bucket, fileKey);

            } catch (Exception e) {
                log.error("Ошибка при разборе события S3: {}", e.getMessage());

                throw new RuntimeException(e);
            }
        }
        if (ack != null) {
            ack.acknowledge();
        }
    }

    private boolean isVaild(S3EventDto.S3Record r) {
        return r != null
                && r.s3() != null
                && r.s3().bucket() != null
                && r.s3().bucket().name() != null
                && r.s3().object() != null
                && r.s3().object().key() != null;
    }
}
