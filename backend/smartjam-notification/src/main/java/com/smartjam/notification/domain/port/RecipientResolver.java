package com.smartjam.notification.domain.port;

import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisType;

public interface RecipientResolver {
    UUID findOwnerId(UUID targetId, AnalysisType type);

    String findFcmToken(UUID userId);
}
