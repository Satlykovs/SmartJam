package com.smartjam.notification.application;

import java.util.List;
import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.notification.domain.port.PushPublisher;
import com.smartjam.notification.domain.port.RecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Core application service (orchestrator) for handling analysis results. Responsible for resolving the recipient's
 * identity and triggering notification delivery through various channels (e.g., Push notifications).
 */
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

                List<String> tokens = recipientResolver.findFcmTokens(userId);

                if (tokens.isEmpty()) {
                    log.warn("User {} has no registered devices, push skipped", userId);
                    return;
                }

                String formattedScore = String.format("%.2f", event.totalScore());

                String message = (event.type() == AnalysisType.SUBMISSION)
                        ? "Твоя игра проанализирована! Балл: " + formattedScore
                        : "Твоя запись успешно обработана! 🎸";
                pushPublisher.sendPush(tokens, message);
            } catch (Exception e) {
                log.error("Failed to send push notification for {}: {}", event.targetId(), e.getMessage());
            }
        }
    }
}
