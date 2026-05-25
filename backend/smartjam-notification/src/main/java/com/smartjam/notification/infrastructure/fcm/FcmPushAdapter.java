package com.smartjam.notification.infrastructure.fcm;

import java.util.List;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.smartjam.notification.domain.port.PushPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production implementation of {@link PushPublisher} using Firebase Cloud Messaging (FCM). Communicates with Google's
 * Firebase Admin SDK to deliver real-time notifications. Active under any profile except 'debug'.
 */
@Component
@Slf4j
@Profile("!debug")
public class FcmPushAdapter implements PushPublisher {

    @Override
    public void sendPush(List<String> fcmTokens, String messageText) {
        try {
            Notification notification = Notification.builder()
                    .setTitle("SmartJam")
                    .setBody(messageText)
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(fcmTokens)
                    .setNotification(notification)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            log.info(
                    "Multicast push sent. Success: {}, Failure: {}",
                    response.getSuccessCount(),
                    response.getFailureCount());

            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(res -> {
                    if (!res.isSuccessful()) {
                        log.warn(
                                "Failed to send to a device: {}",
                                res.getException().getMessage());
                    }
                });
            }

        } catch (Exception e) {

            log.error("Critical error during FCM multicast sending", e);
        }
    }
}
