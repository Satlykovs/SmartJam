package com.smartjam.smartjamanalyzer.domain.port;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;

public interface ResultRepository {

    void save(UUID submissionId, AnalysisResult result);

    UUID findAssignmentIdBySubmissionId(UUID submissionId);

    void updateStatus(UUID id, AudioProcessingStatus status, String errorMessage);
}
