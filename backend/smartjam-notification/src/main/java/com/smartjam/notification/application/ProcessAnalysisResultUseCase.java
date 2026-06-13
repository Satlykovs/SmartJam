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
 * Application service for handling analysis results. Responsible for resolving the recipient's identity and triggering
 * notification delivery through various channels (e.g., Push notifications).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessAnalysisResultUseCase {
    private final RecipientResolver recipientResolver;
    private final PushPublisher pushPublisher;

    public void execute(AnalysisFinishedEvent event) {
        if (event.status() != AudioProcessingStatus.COMPLETED) return;

        try {
            UUID ownerId = recipientResolver.findOwnerId(event.targetId(), event.type());
            sendPushToUser(ownerId, getOwnerMessage(event));
        } catch (Exception e) {
            log.error("Primary notification failed for {}: {}", event.targetId(), e.getMessage());
            throw e;
        }

        try {

            if (event.type() == AnalysisType.REFERENCE) {
                UUID studentId = recipientResolver.findStudentIdByAssignment(event.targetId());
                String teacherUsername = recipientResolver.findTeacherUsernameByAssignment(event.targetId());
                String assignmentTitle = recipientResolver.findAssignmentTitle(event.targetId());

                String msg =
                        String.format("%s добавил урок '%s'! Пора позаниматься 🎸", teacherUsername, assignmentTitle);

                sendPushToUser(studentId, msg);
            }

        } catch (Exception e) {
            log.warn(
                    "Secondary notification failed for {}, skipping retry to avoid duplicates: {}",
                    event.targetId(),
                    e.getMessage());
        }
    }

    private void sendPushToUser(UUID userId, String message) {
        List<String> tokens = recipientResolver.findFcmTokens(userId);
        if (!tokens.isEmpty()) {
            pushPublisher.sendPush(tokens, message);
        }
    }

    private String getOwnerMessage(AnalysisFinishedEvent event) {
        if (event.type() == AnalysisType.SUBMISSION) {
            double score = event.totalScore();
            String formattedScore = String.format("%.1f%%", score);

            if (score >= 90.0) {
                return String.format("Ого! Это было мощно! Балл: %s 🔥 Ты звучишь как профи!", formattedScore);
            } else if (score >= 75.0) {
                return String.format(
                        "Круто! Твоя игра проанализирована. Балл: %s 💪 Хороший результат!", formattedScore);
            } else {
                return String.format(
                        "Анализ готов. Балл: %s 🎸 Немного практики, и всё получится. Давай еще раз!", formattedScore);
            }
        }

        return "Твоя запись успешно обработана и доступна ученику! 🚀";
    }
}
