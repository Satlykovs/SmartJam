package com.smartjam.smartjamanalyzer.domain.port;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;

public interface PerformanceEvaluator {
    AnalysisResult evaluate(FeatureSequence reference, FeatureSequence student);
}
