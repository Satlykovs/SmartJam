package com.smartjam.analyzer.domain.port;

import com.smartjam.analyzer.domain.model.AnalysisResult;

/**
 * Port for generating visual artifacts of the analysis process. Typically used for debugging and fine-tuning DTW
 * thresholds.
 */
public interface DebugVisualizer {

    /**
     * Generates a heatmap image representing the accumulated cost matrix and warping path.
     *
     * @param result The analysis result containing the matrix and path.
     * @param filename The destination file path for the generated image.
     */
    void generateHeatmap(AnalysisResult result, String filename);
}
