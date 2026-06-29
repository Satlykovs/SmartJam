package com.smartjam.notification.api.kafka;

import com.smartjam.common.dto.connection.StudentJoinedEvent;
import com.smartjam.notification.application.ProcessConnectionEventUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Inbound Kafka adapter that listens for connection-related events. Bridges external messaging with internal
 * application logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionEventListener {

    private final ProcessConnectionEventUseCase processConnectionEventUseCase;

    /**
     * Listener for student join events. Uses manual acknowledgment to ensure reliable processing.
     *
     * @param event The student join event received from the message broker.
     * @param ack Kafka acknowledgment handle.
     */
    @KafkaListener(
            topics = "connection-events",
            groupId = "smartjam-notification-group",
            concurrency = "3",
            properties = {"spring.json.value.default.type=com.smartjam.common.dto.connection" + ".StudentJoinedEvent"})
    public void onStudentJoined(StudentJoinedEvent event, Acknowledgment ack) {
        log.info("Received StudentJoinedEvent from Kafka for connection: {}", event.connectionId());

        try {
            processConnectionEventUseCase.handleStudentJoined(event);

            if (ack != null) {
                ack.acknowledge();
                log.debug("Acknowledged StudentJoinedEvent for connection: {}", event.connectionId());
            }
        } catch (Exception e) {
            log.error("Error while processing Connection event from Kafka: {}", e.getMessage(), e);
            throw e;
        }
    }
}
