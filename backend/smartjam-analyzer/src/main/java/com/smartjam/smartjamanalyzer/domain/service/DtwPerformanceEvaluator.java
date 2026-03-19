package com.smartjam.smartjamanalyzer.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.PerformanceEvaluator;

/**
 * Performance evaluator using Dynamic Time Warping (DTW) and Cosine Similarity. Provides granular scoring for pitch and
 * rhythm, along with time-aligned feedback events.
 */
public class DtwPerformanceEvaluator implements PerformanceEvaluator {

    private static final double PITCH_THRESHOLD = 0.4;
    private static final double RHYTHM_TOLERANCE_RATIO = 0.05; // NOTE: надо будет потюнить,
    // вынести в конфиг
    private static final int MAX_GRACE_FRAMES = 5; // ~230мс
    private static final double MIN_ERROR_DURATION_SEC = 0.20;

    private final boolean includeDebugData;

    public DtwPerformanceEvaluator(boolean includeDebugData) {
        this.includeDebugData = includeDebugData;
    }

    @Override
    public AnalysisResult evaluate(FeatureSequence reference, FeatureSequence student) {

        if (reference.binCount() != student.binCount()) {
            throw new IllegalArgumentException(
                    "Несовместимые форматы: " + reference.binCount() + " vs " + student.binCount() + " бинов");
        }

        double[][] dtwMatrix = computeCostMatrix(reference.frames(), student.frames());
        List<int[]> path = findWarpingPath(dtwMatrix);

        List<AnalysisResult.FeedbackEvent> feedbacks = new ArrayList<>();
        PathMetrics metrics = analyzePath(path, reference, student, feedbacks);

        return buildFinalResult(metrics, dtwMatrix, path, feedbacks);
    }

    private PathMetrics analyzePath(
            List<int[]> path,
            FeatureSequence reference,
            FeatureSequence student,
            List<AnalysisResult.FeedbackEvent> feedbacks) {

        ErrorState pitchES = new ErrorState("Wrong note", reference.frameRate(), student.frameRate());
        ErrorState rhythmES = new ErrorState("Wrong rhythm", reference.frameRate(), student.frameRate());

        double totalPitchDist = 0;
        double totalRhythmDrift = 0;
        int n = reference.frames().size();
        int m = student.frames().size();

        for (int[] p : path) {

            double dist = calculateCosineDistance(
                    reference.frames().get(p[0]), student.frames().get(p[1]));
            double xK = (n > 1) ? (double) p[0] / (n - 1) : 0.0;
            double yK = (m > 1) ? (double) p[1] / (m - 1) : 0.0;
            double drift = Math.abs(yK - xK);

            totalPitchDist += dist;
            totalRhythmDrift += drift;

            double tRef = p[0] / reference.frameRate();
            double tStud = p[1] / student.frameRate();

            double pitchSev = Math.clamp((dist - PITCH_THRESHOLD) / (1.0 - PITCH_THRESHOLD), 0.0, 1.0);
            double rhythmSev = Math.clamp((drift - RHYTHM_TOLERANCE_RATIO) / (0.5 - RHYTHM_TOLERANCE_RATIO), 0.0, 1.0);

            processErrorState(pitchES, feedbacks, tRef, tStud, pitchSev, dist > PITCH_THRESHOLD);
            processErrorState(rhythmES, feedbacks, tRef, tStud, rhythmSev, drift > RHYTHM_TOLERANCE_RATIO);
        }

        flushIfActive(pitchES, feedbacks);
        flushIfActive(rhythmES, feedbacks);

        return new PathMetrics(totalPitchDist, totalRhythmDrift);
    }

    private void processErrorState(
            ErrorState state,
            List<AnalysisResult.FeedbackEvent> feedbacks,
            double tRef,
            double tStud,
            double severity,
            boolean isError) {
        state.update(tRef, tStud, severity, isError);
        if (state.shouldFlush()) {
            flushIfActive(state, feedbacks);
        }
    }

    private void flushIfActive(ErrorState state, List<AnalysisResult.FeedbackEvent> feedbacks) {
        if (state.isActive()) {
            AnalysisResult.FeedbackEvent ev = state.flush();
            if (ev != null) feedbacks.add(ev);
        }
    }

    private AnalysisResult buildFinalResult(
            PathMetrics metrics, double[][] matrix, List<int[]> path, List<AnalysisResult.FeedbackEvent> feedbacks) {

        double avgPitchError = metrics.totalPitchDist / path.size();
        double pScore = Math.max(0.0, 100.0 * (1.0 - Math.pow(avgPitchError / 0.5, 2)));

        double avgDrift = metrics.totalRhythmDrift / path.size();
        double rScore = Math.max(0.0, 100.0 * (1.0 - Math.pow(avgDrift / 0.2, 2)));

        return new AnalysisResult(
                Math.sqrt(pScore * rScore), pScore, rScore, path, feedbacks, includeDebugData ? matrix : null);
    }

    double[][] computeCostMatrix(List<float[]> ref, List<float[]> stud) {

        int n = ref.size();
        int m = stud.size();
        double[][] dtw = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                double dist = calculateCosineDistance(ref.get(i), stud.get(j));

                if (i == 0 && j == 0) dtw[i][j] = dist;
                else if (i == 0) dtw[i][j] = dtw[i][j - 1] + dist;
                else if (j == 0) dtw[i][j] = dtw[i - 1][j] + dist;
                else dtw[i][j] = Math.min(dtw[i - 1][j], Math.min(dtw[i][j - 1], dtw[i - 1][j - 1])) + dist;
            }
        }
        return dtw;
    }

    List<int[]> findWarpingPath(double[][] dtw) {

        List<int[]> path = new ArrayList<>();
        int i = dtw.length - 1;
        int j = dtw[0].length - 1;

        while (i > 0 || j > 0) {
            path.add(new int[] {i, j});

            double diag = (i > 0 && j > 0) ? dtw[i - 1][j - 1] : Double.MAX_VALUE;
            double left = (j > 0) ? dtw[i][j - 1] : Double.MAX_VALUE;
            double up = (i > 0) ? dtw[i - 1][j] : Double.MAX_VALUE;

            if (diag <= left && diag <= up) {
                i--;
                j--;
            } else if (left < up) {
                j--;
            } else {
                i--;
            }
        }
        path.add(new int[] {0, 0});
        Collections.reverse(path);
        return path;
    }

    /**
     * Computes the cosine distance between two feature vectors. Handles silence vs silence as a perfect match (0.0
     * distance).
     */
    double calculateCosineDistance(float[] a, float[] b) {

        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int k = 0; k < a.length; k++) {
            dot += a[k] * b[k];
            normA += a[k] * a[k];
            normB += b[k] * b[k];
        }

        if (normA == 0 && normB == 0) return 0.0;

        if (normA == 0 || normB == 0) return 1.0;

        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private record PathMetrics(double totalPitchDist, double totalRhythmDrift) {}

    private static class ErrorState {
        private final String message;
        private final double refFrameDuration;
        private final double studFrameDuration;
        private double rStart = -1;
        private double rEnd = -1;
        private double sStart = -1;
        private double sEnd = -1;
        private double maxSev = 0;
        private int graceCounter = 0;

        ErrorState(String message, float refFrameRate, float studFrameRate) {
            this.message = message;
            this.refFrameDuration = 1.0 / refFrameRate;
            this.studFrameDuration = 1.0 / studFrameRate;
        }

        void update(double tR, double tS, double sev, boolean isErr) {

            if (isErr) {
                if (rStart < 0) {
                    rStart = tR;
                    sStart = tS;
                }

                rEnd = tR;
                sEnd = Math.max(sEnd, tS);
                maxSev = Math.max(maxSev, sev);
                graceCounter = 0;

            } else if (isActive()) {
                graceCounter++;
            }
        }

        boolean shouldFlush() {
            return isActive() && graceCounter >= MAX_GRACE_FRAMES;
        }

        boolean isActive() {
            return rStart >= 0;
        }

        AnalysisResult.FeedbackEvent flush() {
            double refDuration = (rEnd - rStart) + refFrameDuration;
            double studDuration = (sEnd - sStart) + studFrameDuration;

            if (refDuration < MIN_ERROR_DURATION_SEC && studDuration < MIN_ERROR_DURATION_SEC) {
                reset();
                return null;
            }

            AnalysisResult.FeedbackEvent event = new AnalysisResult.FeedbackEvent(
                    rStart, rEnd + refFrameDuration, sStart, sEnd + studFrameDuration, message, maxSev);
            reset();
            return event;
        }

        private void reset() {
            rStart = -1;
            rEnd = -1;
            sStart = -1;
            sEnd = -1;
            maxSev = 0;
            graceCounter = 0;
        }
    }
}
