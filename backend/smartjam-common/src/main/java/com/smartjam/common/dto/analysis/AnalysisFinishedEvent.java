package com.smartjam.common.dto.analysis;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import lombok.Builder;

@Builder
public record AnalysisFinishedEvent(
        UUID targetId, AnalysisType type, AudioProcessingStatus status, Double totalScore, String errorMessage) {}
