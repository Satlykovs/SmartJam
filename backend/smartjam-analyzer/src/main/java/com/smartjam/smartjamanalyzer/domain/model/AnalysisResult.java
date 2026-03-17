package com.smartjam.smartjamanalyzer.domain.model;

import java.util.List;

public record AnalysisResult(
        double totalScore,
        double pitchScore,
        double rhythmScore,
        List<int[]> warpingPath,
        List<FeedbackEvent> feedback,
        double[][] costMatrix) {

    public boolean isPassed() {
        return totalScore > 80;
    }

    public record FeedbackEvent(
            double teacherStartTime,
            double teacherEndTime,
            double studentStartTime,
            double studentEndTime,
            String message,
            double severity) {}
}
