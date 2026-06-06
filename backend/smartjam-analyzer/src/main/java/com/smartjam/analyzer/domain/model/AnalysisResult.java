package com.smartjam.analyzer.domain.model;

import java.util.List;

import com.smartjam.common.model.FeedbackEvent;

/**
 * Encapsulates the results of a performance evaluation, including overall scores, time-aligned feedback events, and
 * internal evaluation artifacts (like warping paths).
 *
 * @param totalScore Overall performance score (0.0 - 100.0).
 * @param pitchScore Score representing pitch accuracy (0.0 - 100.0).
 * @param rhythmScore Score representing rhythmic stability (0.0 - 100.0).
 * @param warpingPath List of [teacherIndex, studentIndex] pairs representing the optimal DTW path.
 * @param feedback List of specific error events detected during evaluation.
 * @param teacherWaveform RMS energy vector of the teacher's reference audio.
 * @param studentWaveform RMS energy vector of the student's submission audio.
 * @param costMatrix The accumulated cost matrix (nullable, used only for debug visualization).
 */
public record AnalysisResult(
        double totalScore,
        double pitchScore,
        double rhythmScore,
        List<int[]> warpingPath,
        List<FeedbackEvent> feedback,
        float[] teacherWaveform,
        float[] studentWaveform,
        double[][] costMatrix) {

    public AnalysisResult {
        warpingPath = warpingPath == null
                ? List.of()
                : warpingPath.stream().map(int[]::clone).toList();
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
        teacherWaveform = (teacherWaveform != null) ? teacherWaveform.clone() : new float[0];
        studentWaveform = (studentWaveform != null) ? studentWaveform.clone() : new float[0];

        costMatrix = copyMatrix(costMatrix);
    }

    private static double[][] copyMatrix(double[][] src) {
        if (src == null) return null;
        double[][] out = new double[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i] == null ? null : src[i].clone();
        }
        return out;
    }

    public boolean isPassed() {
        return totalScore > 80;
    }
}
