package com.smartjam.smartjamapi.repository;

import java.util.UUID;

import com.smartjam.smartjamapi.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository interface for managing {@link DeviceEntity} persistence. */
public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {

    /**
     * Safely deletes a device registration record. Verification by both token and userId prevents unauthorized removal
     * of tokens.
     *
     * @param fcmToken the unique Firebase token to remove
     * @param userId the ID of the user who should own this token
     */
    void deleteByFcmTokenAndUserId(String fcmToken, UUID userId);
}
