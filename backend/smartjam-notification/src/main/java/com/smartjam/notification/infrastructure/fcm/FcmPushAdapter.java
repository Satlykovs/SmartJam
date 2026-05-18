package com.smartjam.notification.infrastructure.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.smartjam.notification.domain.port.PushPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
            log.error("Firebase cloud messaging error for token {}: {}", fcmToken, e.getMessage(), e);
        }
    }
}
