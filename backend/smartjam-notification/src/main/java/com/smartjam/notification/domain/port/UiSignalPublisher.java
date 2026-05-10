package com.smartjam.notification.domain.port;

import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisType;

public interface UiSignalPublisher {
    void sendRefreshSignal(UUID targetId, AnalysisType type);
}
