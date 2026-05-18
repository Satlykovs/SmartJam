package com.smartjam.notification.domain.port;

public interface PushPublisher {
    void sendPush(String fcmToken, String message);
}
