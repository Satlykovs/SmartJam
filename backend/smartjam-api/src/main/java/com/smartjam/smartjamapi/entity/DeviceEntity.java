package com.smartjam.smartjamapi.entity;

import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.smartjamapi.enums.DeviceType;
import lombok.*;

/**
 * JPA entity representing a user's device registration. Stores the FCM (Firebase Cloud Messaging) token used to deliver
 * push notifications.
 */
@Entity
@Table(name = "user_devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEntity {

    /** Unique registration token provided by the Firebase SDK. Acts as the Primary Key. */
    @Id
    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    /** Unique identifier of the user who owns this device. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Type of the device (e.g., ANDROID, IOS, WEB). Defaults to {@link DeviceType#ANDROID}. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 50)
    private DeviceType deviceType = DeviceType.ANDROID; // Just because we have only android app
    //     right now

}
