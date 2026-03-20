package com.smartjam.smartjamanalyzer.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("debug")
@Disabled("TODO: Enable later. Need something to run a database in github CI/CD")
class AudioAnalysisIntegrationTest {

    @Autowired
    private FeatureExtractor extractor;

    @Autowired
    private PerformanceEvaluator evaluator;

    @Autowired
    private DebugVisualizer visualizer;

    @Autowired
    private AudioConverter audioConverter;

    @Autowired
    private WorkspaceFactory workspaceFactory;

    @Test
    @DisplayName("Полный цикл анализа с замером производительности")
    void shouldPerformFullAnalysisCycle() throws Exception {
        Path teacherPath = Path.of("src/test/resources/californication_teacher.m4a");
        Path studentPath = Path.of("src/test/resources/californication_stud.m4a");

        StopWatch sw = new StopWatch("Audio Pipeline Benchmark");

        try (Workspace workspace = workspaceFactory.create()) {

            sw.start("FFmpeg Conversion");
            Path tWav = audioConverter.convertToStandardWav(teacherPath, workspace);
            Path sWav = audioConverter.convertToStandardWav(studentPath, workspace);
            sw.stop();

            sw.start("Feature Extraction (CQT)");
            FeatureSequence tSeq = extractor.extract(tWav);
            FeatureSequence sSeq = extractor.extract(sWav);
            sw.stop();

            sw.start("DTW Mathematical Core");
            AnalysisResult result = evaluator.evaluate(tSeq, sSeq);
            sw.stop();

            String feedbackReport = result.feedback().stream()
                    .map(e -> String.format(
                            "-> [%s] с %5.2fs до %5.2fs (Ученик: с %5.2fs до %5.2fs) | Тяжесть: %.2f",
                            e.type(),
                            e.teacherStartTime(),
                            e.teacherEndTime(),
                            e.studentStartTime(),
                            e.studentEndTime(),
                            e.severity()))
                    .collect(Collectors.joining("\n"));

            String report = String.format(
                    """

                ===========================================================
                АНАЛИЗ ЗАВЕРШЕН: %s vs %s
                ===========================================================
                МЕТРИКИ КАЧЕСТВА:
                -> Общий балл:    %6.2f%%
                -> Точность нот:  %6.2f%%
                -> Ритм и темп:   %6.2f%%
                -> Статус:        %s
                -----------------------------------------------------------
                НАЙДЕННЫЕ ОШИБКИ (%d):
                %s
                -----------------------------------------------------------
                ПРОИЗВОДИТЕЛЬНОСТЬ:
                %s
                ===========================================================
                """,
                    teacherPath.getFileName(),
                    studentPath.getFileName(),
                    result.totalScore(),
                    result.pitchScore(),
                    result.rhythmScore(),
                    result.isPassed() ? "PASSED" : "FAILED",
                    result.feedback().size(),
                    feedbackReport.isEmpty() ? "Ошибок не найдено." : feedbackReport,
                    sw.prettyPrint());

            log.info(report);

            Path heatmapPath = Path.of("audio_compare_debug.png");
            visualizer.generateHeatmap(result, heatmapPath.toString());

            assertTrue(Files.exists(heatmapPath));
        }
    }
}
