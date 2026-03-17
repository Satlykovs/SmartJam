package com.smartjam.smartjamanalyzer.infrastructure.visualizer;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.port.DebugVisualizer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!debug")
public class NoOpDebugVisualizer implements DebugVisualizer {
    @Override
    public void generateHeatmap(AnalysisResult result, String filename) {}
}
