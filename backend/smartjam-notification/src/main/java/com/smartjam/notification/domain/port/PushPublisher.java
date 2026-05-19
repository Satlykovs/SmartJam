package com.smartjam.notification.domain.port;

/**
 * Outbound port for sending push notifications. Abstracts the underlying delivery mechanism (like Firebase or APNs).
 */
public interface PushPublisher {
    /**
     * Sends a push notification to a specific device.
     *
     * @param fcmToken The target device's registration token.
     * @param message The text content of the notification.
     */
    void sendPush(String fcmToken, String message);
}
