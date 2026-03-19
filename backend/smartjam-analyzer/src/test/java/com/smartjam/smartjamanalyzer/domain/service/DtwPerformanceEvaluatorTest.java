package com.smartjam.smartjamanalyzer.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit-тестирование ядра DTW")
class DtwPerformanceEvaluatorTest {
    private final DtwPerformanceEvaluator evaluator = new DtwPerformanceEvaluator(false);

    private float[] gaussianPeak(int center, double sigma, int size) {
        float[] vec = new float[size];
        for (int i = 0; i < size; i++) {
            double dist = Math.abs(i - center);
            vec[i] = (float) Math.exp(-dist * dist / (2 * sigma * sigma));
        }
        return vec;
    }

    @Test
    @DisplayName("Базовый тест: идентичные последовательности")
    void shouldReturnPerfectScoreForIdenticalSequences() {
        float[] frame = new float[84];
        frame[12] = 1.0f;
        List<float[]> frames = Collections.nCopies(3, frame);
        FeatureSequence seq = new FeatureSequence(frames, 20.0f);

        AnalysisResult result = evaluator.evaluate(seq, seq);

        assertEquals(100.0, result.totalScore(), 0.001);
        assertTrue(result.feedback().isEmpty());
    }

    @Test
    @DisplayName("Pitch Score: проверка на разных нотах")
    void shouldDetectPitchErrorRealistic() {
        double sigma = 1.3;
        float[] refFrame = gaussianPeak(12, sigma, 84);
        float[] studFrame = gaussianPeak(14, sigma, 84);

        List<float[]> ref = Collections.nCopies(20, refFrame);
        List<float[]> stud = Collections.nCopies(20, studFrame);

        AnalysisResult result = evaluator.evaluate(new FeatureSequence(ref, 20.0f), new FeatureSequence(stud, 20.0f));

        assertTrue(result.pitchScore() < 50.0, "Score должен упасть при неверных нотах");
        assertTrue(result.pitchScore() > 20.0, "Score не должен быть нулевым при перекрытии");
        assertFalse(result.feedback().isEmpty());
    }

    @Test
    @DisplayName("Математика: Косинусное расстояние")
    void testCosineDistance() {
        float[] v1 = {1, 0, 0};
        float[] v2 = {1, 0, 0};
        float[] v3 = {0, 1, 0};

        assertAll(
                "Расстояния",
                () -> assertEquals(0.0, evaluator.calculateCosineDistance(v1, v2), 1e-6),
                () -> assertEquals(1.0, evaluator.calculateCosineDistance(v1, v3), 1e-6));
    }

    @Test
    @DisplayName("Ритм: равномерное замедление даёт высокую оценку")
    void testRhythmStability() {
        List<float[]> refFrames = new ArrayList<>();
        List<float[]> studFrames = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            float val = (float) i / 100;
            refFrames.add(new float[] {val, 1.0f});
            if (i % 2 == 0) {
                studFrames.add(new float[] {val, 1.0f});
            }
        }

        FeatureSequence ref = new FeatureSequence(refFrames, 10f);
        FeatureSequence stud = new FeatureSequence(studFrames, 10f);

        AnalysisResult result = evaluator.evaluate(ref, stud);

        assertTrue(result.rhythmScore() > 95.0);
    }

    @Test
    @DisplayName("Ученик сыграл только начало")
    void shouldPenalizeEarlyTermination() {
        List<float[]> ref = new ArrayList<>();
        for (int i = 0; i < 100; i++) ref.add(new float[] {1.0f, (float) i / 100});

        List<float[]> stud = new ArrayList<>();
        for (int i = 0; i < 30; i++) stud.add(new float[] {1.0f, (float) i / 100});

        AnalysisResult result = evaluator.evaluate(new FeatureSequence(ref, 20f), new FeatureSequence(stud, 20f));

        assertTrue(result.totalScore() < 50.0, "Score должен быть низким при обрыве записи");
        boolean hasRhythmError =
                result.feedback().stream().anyMatch(e -> e.message().equals("Wrong rhythm"));
        assertTrue(hasRhythmError);
    }

    @Test
    @DisplayName("Склейка мелких ошибок через Grace Frames")
    void shouldMergeFrequentShortErrorsRealistic() {
        double sigma = 0.5;
        float[] good = gaussianPeak(40, sigma, 84);
        float[] bad = gaussianPeak(50, sigma, 84);

        List<float[]> ref = new ArrayList<>();
        List<float[]> stud = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            ref.add(good);
            if (i % 3 == 0) {
                stud.add(good);
            } else {
                stud.add(bad);
            }
        }

        AnalysisResult result = evaluator.evaluate(new FeatureSequence(ref, 20f), new FeatureSequence(stud, 20f));

        List<AnalysisResult.FeedbackEvent> pitchEvents = result.feedback().stream()
                .filter(e -> e.message().equals("Wrong note"))
                .toList();
        assertTrue(pitchEvents.size() < 5, "Ошибки должны склеиваться, лог не должен спамить");
    }

    @Test
    @DisplayName("Ноты отличаются на полутон, результат должен быть ненулевым")
    void shouldBeSensitiveToSmallPitchShiftsRealistic() {
        float[] refFrame = gaussianPeak(40, 0.7, 84);
        float[] studFrame = gaussianPeak(41, 0.7, 84);

        List<float[]> ref = Collections.nCopies(50, refFrame);
        List<float[]> stud = Collections.nCopies(50, studFrame);

        AnalysisResult result = evaluator.evaluate(new FeatureSequence(ref, 20f), new FeatureSequence(stud, 20f));

        assertTrue(
                result.pitchScore() > 20.0 && result.pitchScore() < 80.0,
                "Оценка должна быть промежуточной при реалистичных признаках");
        boolean hasPitchError =
                result.feedback().stream().anyMatch(e -> e.message().equals("Wrong note"));
        assertTrue(hasPitchError);
    }

    @Test
    @DisplayName("Severity для небольшой ошибки не максимальна")
    void severityShouldNotBeMaxForSmallError() {
        double sigma = 1.5;
        float[] refFrame = gaussianPeak(40, sigma, 84);
        float[] studFrame = gaussianPeak(41, sigma, 84);

        List<float[]> ref = Collections.nCopies(50, refFrame);
        List<float[]> stud = Collections.nCopies(50, studFrame);

        AnalysisResult result = evaluator.evaluate(new FeatureSequence(ref, 20f), new FeatureSequence(stud, 20f));

        result.feedback().stream()
                .filter(e -> e.message().equals("Wrong note"))
                .forEach(e -> assertTrue(e.severity() < 0.9, "Severity должна быть <0.9"));
    }

    @Test
    @DisplayName("Защита от мусора: Ошибки короче 200мс игнорируются")
    void shouldIgnoreVeryShortNoise() {
        List<float[]> ref = new ArrayList<>(Collections.nCopies(100, new float[] {1f, 0f}));
        List<float[]> stud = new ArrayList<>(Collections.nCopies(100, new float[] {1f, 0f}));
        stud.set(50, new float[] {0f, 1f});

        AnalysisResult result = evaluator.evaluate(new FeatureSequence(ref, 20.0f), new FeatureSequence(stud, 20.0f));

        assertTrue(result.totalScore() > 95.0);
        assertTrue(result.feedback().isEmpty(), "Одиночный битый кадр не должен генерировать Event");
    }

    @Test
    @DisplayName("Grace period: короткие перерывы склеиваются")
    void shortGapsShouldMergeRealistic() {
        double sigma = 0.5;
        float[] good = gaussianPeak(40, sigma, 84);
        float[] bad = gaussianPeak(50, sigma, 84);

        List<float[]> ref = new ArrayList<>();
        List<float[]> stud = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ref.add(good);
            if (i % 3 == 0) {
                stud.add(good);
            } else {
                stud.add(bad);
            }
        }

        FeatureSequence refSeq = new FeatureSequence(ref, 20f);
        FeatureSequence studSeq = new FeatureSequence(stud, 20f);

        AnalysisResult result = evaluator.evaluate(refSeq, studSeq);

        List<AnalysisResult.FeedbackEvent> pitchEvents = result.feedback().stream()
                .filter(e -> e.message().equals("Wrong note"))
                .toList();
        assertEquals(1, pitchEvents.size(), "Ошибки должны склеиться в одно событие");
        AnalysisResult.FeedbackEvent event = pitchEvents.getFirst();
        assertTrue(event.studentEndTime() - event.studentStartTime() > 2.0);
    }

    @Test
    @DisplayName("Grace period: длинный перерыв разрывает событие")
    void longGapShouldSplitRealistic() {
        double sigma = 0.5;
        float[] good = gaussianPeak(40, sigma, 84);
        float[] bad = gaussianPeak(50, sigma, 84);

        List<float[]> ref = new ArrayList<>();
        List<float[]> stud = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ref.add(good);
            stud.add(bad);
        }
        for (int i = 0; i < 10; i++) {
            ref.add(good);
            stud.add(good);
        }
        for (int i = 0; i < 10; i++) {
            ref.add(good);
            stud.add(bad);
        }

        FeatureSequence refSeq = new FeatureSequence(ref, 20f);
        FeatureSequence studSeq = new FeatureSequence(stud, 20f);

        AnalysisResult result = evaluator.evaluate(refSeq, studSeq);

        List<AnalysisResult.FeedbackEvent> pitchEvents;
        pitchEvents = result.feedback().stream()
                .filter(e -> e.message().equals("Wrong note"))
                .toList();
        assertEquals(2, pitchEvents.size(), "Длинный перерыв должен разделить ошибки");
    }
}
