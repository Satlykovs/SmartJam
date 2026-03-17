package com.smartjam.smartjamanalyzer.domain.port;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;

public interface DebugVisualizer {
    void generateHeatmap(AnalysisResult result, String filename);
}
