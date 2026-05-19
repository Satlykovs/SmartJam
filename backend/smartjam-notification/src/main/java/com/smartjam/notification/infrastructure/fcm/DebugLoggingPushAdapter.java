package com.smartjam.notification.infrastructure.fcm;

import com.smartjam.notification.domain.port.PushPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of {@link PushPublisher} for local development and testing. Redirects notifications to the
 * application logs instead of sending real requests to Google. Active only when the 'debug' profile is enabled.
 */
@Slf4j
@Component
@Profile("debug")
public class DebugLoggingPushAdapter implements PushPublisher {
    @Override
    public void sendPush(String fcmToken, String message) {
        String maskedToken = (fcmToken != null && fcmToken.length() > 10) ? fcmToken.substring(0, 10) + "..." : "***";
        log.info("[MOCK PUSH] Sending to User {}: {}", maskedToken, message);
    }
}
