package com.smartjam.smartjamapi.service;

import java.util.UUID;

import jakarta.transaction.Transactional;

import com.smartjam.smartjamapi.entity.DeviceEntity;
import com.smartjam.smartjamapi.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service layer responsible for business logic related to device management. Coordinates registration and
 * unregistration of notification tokens.
 */
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;

    /**
     * Registers a new device token or updates an existing one for the current user. Uses the "Last Device Wins"
     * strategy for token-user mapping.
     *
     * @param fcmToken the registration token received from the mobile client
     */
    @Transactional
    public void register(String fcmToken) {
        UUID userId = getCurrentUserId();

        DeviceEntity device =
                DeviceEntity.builder().fcmToken(fcmToken).userId(userId).build();

        deviceRepository.save(device);
    }

    /**
     * Removes a device registration, effectively disabling push notifications for that device. Usually called when the
     * user logs out.
     *
     * @param fcmToken the token to be invalidated
     */
    @Transactional
    public void unregister(String fcmToken) {
        UUID userId = getCurrentUserId();
        deviceRepository.deleteByFcmTokenAndUserId(fcmToken, userId);
    }

    private UUID getCurrentUserId() {
        String userIdStr =
                SecurityContextHolder.getContext().getAuthentication().getName();
        return UUID.fromString(userIdStr);
    }
}
