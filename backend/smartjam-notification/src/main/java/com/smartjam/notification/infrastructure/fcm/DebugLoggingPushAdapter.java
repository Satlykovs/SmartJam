package com.smartjam.notification.infrastructure.fcm;

import java.util.UUID;

import com.smartjam.notification.domain.port.PushPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DebugLoggingPushAdapter implements PushPublisher {
    @Override
    public void sendPush(UUID userId, String message) {
        log.info("[MOCK PUSH] Sending to User {}: {}", userId, message);
    }
}
