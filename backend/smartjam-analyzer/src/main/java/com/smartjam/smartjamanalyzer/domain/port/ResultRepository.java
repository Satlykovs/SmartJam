package com.smartjam.smartjamanalyzer.domain.port;

import java.util.UUID;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;

public interface ResultRepository {
    void save(UUID submissionId, AnalysisResult result);
}
