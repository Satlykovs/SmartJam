package com.smartjam.smartjamanalyzer.application;

import java.nio.file.Path;
import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioAnalysisUseCase {

    private final AudioStorage audioStorage;
    private final AudioConverter audioConverter;
    private final WorkspaceFactory workspaceFactory;
    private final FeatureExtractor featureExtractor;

    private final PerformanceEvaluator performanceEvaluator;
    private final ReferenceRepository referenceRepository;
    private final ResultRepository resultRepository;
    private final DebugVisualizer debugVisualizer;

    public void execute(String bucket, String fileKey) {
        UUID entityId = extractUuid(fileKey);

        // TODO: Добавить обработку(проверку типа) входящего файла

        // TODO: Добавить нормальный сбор метрик

        try (Workspace workspace = workspaceFactory.create()) {
            updateStatus(bucket, entityId, AudioProcessingStatus.ANALYZING, null);

            StopWatch watch = new StopWatch(fileKey);

            log.info("=== Начало обработки файла: {} из бакета {} ===", fileKey, bucket);

            watch.start("Download S3");
            Path localFile = audioStorage.downloadAudioFile(bucket, fileKey, workspace);
            watch.stop();

            watch.start("FFmpeg convert");
            Path cleanWavFile = audioConverter.convertToStandardWav(localFile, workspace);
            watch.stop();

            watch.start("Feature Extraction");
            FeatureSequence features = featureExtractor.extract(cleanWavFile);
            watch.stop();

            log.info("Extracted {} feature frames", features.frames().size());

            watch.start("Evaluation & Persistence");
            if ("references".equals(bucket)) {
                handleTeacherReference(entityId, features);
            } else if ("submissions".equals(bucket)) {
                handleStudentSubmission(entityId, features);
            }
            watch.stop();

            log.info("Результаты обработки {}: \n{}", fileKey, watch.prettyPrint());

        } catch (Exception e) {

            String errorMsg =
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            log.error("Ошибка в UseCase для файла {}: {}", fileKey, errorMsg, e);
            updateStatus(bucket, entityId, AudioProcessingStatus.FAILED, e.getMessage());

            throw new RuntimeException("Business logic failed", e);
        }
    }

    private void updateStatus(String bucket, UUID id, AudioProcessingStatus status, String error) {
        try {
            if ("references".equals(bucket)) {
                referenceRepository.updateStatus(id, status, error);
            } else {
                resultRepository.updateStatus(id, status, error);
            }
        } catch (Exception ex) {
            log.error("Failed to update status in DB for {}: {}", id, ex.getMessage());
        }
    }

    private void handleTeacherReference(UUID assignmentId, FeatureSequence teacherFeatures) {
        log.info("Saving teacher reference features for assignment: {}", assignmentId);
        referenceRepository.save(assignmentId, teacherFeatures);
    }

    private void handleStudentSubmission(UUID submissionId, FeatureSequence studentFeatures) {
        log.info("Evaluating student submission: {}", submissionId);

        UUID assignmentId = resultRepository.findAssignmentIdBySubmissionId(submissionId);

        FeatureSequence teacherFeatures = referenceRepository
                .findFeaturesById(assignmentId)
                .orElseThrow((() -> new IllegalArgumentException(
                        "Teacher reference features not found for assignment: " + assignmentId)));

        AnalysisResult result = performanceEvaluator.evaluate(teacherFeatures, studentFeatures);

        resultRepository.save(submissionId, result);

        try {
            debugVisualizer.generateHeatmap(result, "debug_" + submissionId + ".png");
        } catch (Exception e) {
            log.error("Не удалось сгенерировать тепловую карту {}: {}", submissionId, e.getMessage());
        }

        log.info("Submission {} evaluation completed.", submissionId);
    }

    private UUID extractUuid(String fileKey) {
        try {
            String rawUuid = fileKey.contains(".") ? fileKey.substring(0, fileKey.lastIndexOf(".")) : fileKey;

            return UUID.fromString(rawUuid);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid S3 file key. Expected UUID, got: " + fileKey);
        }
    }
}
