package com.smartjam.smartjamapi.listener;

import com.smartjam.smartjamapi.record.S3WebhookPayload;
import com.smartjam.smartjamapi.service.S3WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class S3AvatarEventListener {

    private final S3WebhookService s3WebhookService;

    @KafkaListener(
            topics = "s3-avatar-events",
            groupId = "smartjam-avatar-processor",
            concurrency = "3",
            properties = {
                "spring.json.use.type.headers=false",
                "spring.json.value.default.type=com.smartjam.smartjamapi.record.S3WebhookPayload"
            })
    public void onAvatarEvent(S3WebhookPayload payload) {
        log.info("Received avatar event: {} records", payload.records().size());
        try {
            s3WebhookService.validateUserAvatar(payload);
        } catch (Exception e) {
            log.error("Failed to process avatar event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
