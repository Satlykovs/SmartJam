package com.smartjam.smartjamanalyzer.infrastructure.visualizer;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.port.DebugVisualizer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * A "Null Object" implementation of {@link DebugVisualizer}. This component is active when the "debug" profile is NOT
 * present. It ensures the application doesn't waste CPU and Disk resources on image generation in production
 * environments, while satisfying dependency injection requirements.
 */
@Component
@Profile("!debug")
public class NoOpDebugVisualizer implements DebugVisualizer {

    /**
     * A no-operation implementation. Does nothing.
     *
     * @param result The analysis result (ignored).
     * @param filename The destination path (ignored).
     */
    @Override
    public void generateHeatmap(AnalysisResult result, String filename) {
        // No-op: heatmaps are disabled in current environment profile.

    }
}
