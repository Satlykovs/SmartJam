package com.smartjam.notification.infrastructure.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
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
    public void sendPush(String fcmToken, String messageText) {
        try {
            Notification notification = Notification.builder()
                    .setTitle("SmartJam")
                    .setBody(messageText)
                    .build();

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);

            log.info("Successfully sent push notification. Firebase response: {}", response);

        } catch (Exception e) {

            String maskedToken =
                    (fcmToken != null && fcmToken.length() > 10) ? fcmToken.substring(0, 10) + "..." : "***";
            log.error("Firebase cloud messaging error for token {}: {}", maskedToken, e.getMessage(), e);
        }
    }
}
