package com.smartjam.notification.domain.port;

import java.util.UUID;

public interface PushPublisher {
    void sendPush(UUID userId, String message);
}
