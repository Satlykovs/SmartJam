package com.smartjam.notification.domain.port;

import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisType;

/**
 * Outbound port for looking up notification recipients. Links technical entity IDs (assignments/submissions) to real
 * users and their devices.
 */
public interface RecipientResolver {
    /** Finds the owner ID for the given target (Student or Teacher). */
    UUID findOwnerId(UUID targetId, AnalysisType type);

    /** Retrieves the FCM registration token for a specific user. */
    String findFcmToken(UUID userId);
}
