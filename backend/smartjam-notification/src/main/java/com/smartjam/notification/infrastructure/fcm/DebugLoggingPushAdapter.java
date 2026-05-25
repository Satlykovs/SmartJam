package com.smartjam.notification.infrastructure.fcm;

import java.util.List;
import java.util.stream.Collectors;

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
    public void sendPush(List<String> fcmTokens, String message) {
        String tokensPreview = fcmTokens.stream()
                .map(t -> (t.length() > 10) ? t.substring(0, 10) + "..." : "***")
                .collect(Collectors.joining(", "));

        log.info(
                "[MOCK PUSH] Sending to {} devices. Tokens: [{}]. Message: {}",
                fcmTokens.size(),
                tokensPreview,
                message);
    }
}
