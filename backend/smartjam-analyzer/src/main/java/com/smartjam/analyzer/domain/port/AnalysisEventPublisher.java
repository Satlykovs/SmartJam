package com.smartjam.analyzer.domain.port;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;

public interface AnalysisEventPublisher {
    void publish(AnalysisFinishedEvent event);
}
