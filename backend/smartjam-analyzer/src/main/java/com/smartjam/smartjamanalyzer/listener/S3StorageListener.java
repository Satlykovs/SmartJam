package com.smartjam.smartjamanalyzer.listener;


import com.smartjam.smartjamanalyzer.dto.S3EventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class S3StorageListener
{
    @KafkaListener(topics = "s3-events", groupId = "smartjam-analyzer-group", concurrency = "3",
            properties = {"spring.json.value.default.type=com.smartjam.smartjamanalyzer.dto" +
                    ".S3EventDto"})
    public void onFileUploaded(S3EventDto event)
    {
        if (event.records() == null || event.records().isEmpty())
        {
            log.warn("AADADAD");
            return;
        }
        for (S3EventDto.S3Record record : event.records())
        {
            try
            {
                var s3Data = record.s3();
                String bucket = s3Data.bucket().name();

                String fileKey = URLDecoder.decode(s3Data.object().key(), StandardCharsets.UTF_8);

                log.info("-------------------------------------------------------");
                log.info("🔔 [S3 EVENT] Поймали новое событие: {}", record.eventName());
                log.info("📍 Бакет: {}", bucket);
                log.info("📄 Файл: {}", fileKey);


                if ("references".equals(bucket))
                {
                    log.info("🛠 Действие: Обработка ЭТАЛОНА учителя...");
                } else if ("submissions".equals(bucket))
                {
                    log.info("🛠 Действие: Обработка ПОПЫТКИ ученика...");
                }
                log.info("-------------------------------------------------------");
            } catch (Exception e)
            {
                log.error("❌ Ошибка при разборе события S3: {}", e.getMessage());
            }
        }
    }
}
