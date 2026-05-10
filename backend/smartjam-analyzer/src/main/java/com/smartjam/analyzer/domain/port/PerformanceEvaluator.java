package com.smartjam.analyzer.domain.port;

import com.smartjam.analyzer.domain.model.AnalysisResult;
import com.smartjam.analyzer.domain.model.FeatureSequence;

public interface PerformanceEvaluator {
    AnalysisResult evaluate(FeatureSequence reference, FeatureSequence student);
}
