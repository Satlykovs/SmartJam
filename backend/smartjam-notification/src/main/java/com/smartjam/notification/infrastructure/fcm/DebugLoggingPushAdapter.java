package com.smartjam.notification.infrastructure.fcm;

import com.smartjam.notification.domain.port.PushPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("debug")
public class DebugLoggingPushAdapter implements PushPublisher {
    @Override
    public void sendPush(String fcmToken, String message) {
        log.info("[MOCK PUSH] Sending to User {}: {}", fcmToken, message);
    }
}
