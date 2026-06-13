package com.smartjam.notification.application;

import java.util.List;
import java.util.UUID;

import com.smartjam.common.dto.connection.StudentJoinedEvent;
import com.smartjam.notification.domain.port.PushPublisher;
import com.smartjam.notification.domain.port.RecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for handling connection-related events. Coordinates the process of identifying the correct
 * recipients for connection updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessConnectionEventUseCase {

    private final RecipientResolver recipientResolver;
    private final PushPublisher pushPublisher;

    /**
     * Handles the event when a student successfully joins a teacher's connection.
     *
     * @param event The event data containing connection ID and student's display name.
     */
    public void handleStudentJoined(StudentJoinedEvent event) {
        log.info("Handling student joined event for connection ID: {}", event.connectionId());

        try {
            UUID teacherId = recipientResolver.findTeacherIdByConnection(event.connectionId());

            List<String> tokens = recipientResolver.findFcmTokens(teacherId);

            if (tokens.isEmpty()) {
                log.warn("Teacher with ID {} has no registered devices, skipping push notification", teacherId);
                return;
            }

            String message = String.format("К тебе добавился новый ученик: %s! 👋", event.studentName());
            pushPublisher.sendPush(tokens, message);

            log.debug("Push notification sent to teacher {}", teacherId);

        } catch (Exception e) {
            log.error(
                    "Failed to process student join notification for connection {}: {}",
                    event.connectionId(),
                    e.getMessage(),
                    e);

            throw e;
        }
    }
}
