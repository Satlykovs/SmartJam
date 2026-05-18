package com.smartjam.notification.application;

import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.notification.domain.port.PushPublisher;
import com.smartjam.notification.domain.port.RecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessAnalysisResultUseCase {
    private final RecipientResolver recipientResolver;
    private final PushPublisher pushPublisher;

    public void execute(AnalysisFinishedEvent event) {
        log.info(
                "Processing analysis result for target ID: {}, type: {}, status: {}",
                event.targetId(),
                event.type(),
                event.status());

        if (event.status() == AudioProcessingStatus.COMPLETED) {
            try {
                UUID userId = recipientResolver.findOwnerId(event.targetId(), event.type());

                String token = recipientResolver.findFcmToken(userId);

                if (token == null || token.isEmpty()) {
                    log.warn("User {} does not have fcm token, push will not be send", userId);
                    return;
                }

                String message = (event.type() == AnalysisType.SUBMISSION)
                        ? "Твоя игра " + "проанализирована! Балл: " + event.totalScore()
                        : "Твоя запись " + "обработана!";
                pushPublisher.sendPush(token, message);
            } catch (Exception e) {
                log.error("Failed to send push notification for {}: {}", event.targetId(), e.getMessage());
            }
        }
    }
}
