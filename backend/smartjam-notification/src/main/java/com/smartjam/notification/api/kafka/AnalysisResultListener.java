package com.smartjam.notification.api.kafka;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.notification.application.ProcessAnalysisResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisResultListener {
    private final ProcessAnalysisResultUseCase analysisResultUseCase;

    @KafkaListener(
            topics = "analysis-results",
            groupId = "smartjam-notification-group",
            concurrency = "3",
            properties = {"spring.json.value.default.type=com.smartjam.common.dto" + ".analysis.AnalysisFinishedEvent"})
    public void onAnalysisFinished(AnalysisFinishedEvent event, Acknowledgment ack) {
        log.info("Received analysis result event from Kafka for ID: {}", event.targetId());

        try {
            analysisResultUseCase.execute(event);
            if (ack != null) ack.acknowledge();

            log.debug("Acknowledged message for ID: {}", event.targetId());
        } catch (Exception e) {
            log.error("Failed to process analysis result for ID: {}. Error: {}", event.targetId(), e.getMessage(), e);

            throw e;
        }
    }
}
