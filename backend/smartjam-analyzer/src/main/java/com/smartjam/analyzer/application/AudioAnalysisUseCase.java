package com.smartjam.analyzer.application;

import java.nio.file.Path;
import java.util.UUID;

import com.smartjam.analyzer.domain.exception.AnalysisFatalException;
import com.smartjam.analyzer.domain.model.AnalysisResult;
import com.smartjam.analyzer.domain.model.FeatureSequence;
import com.smartjam.analyzer.domain.port.*;
import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.common.model.AudioProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioAnalysisUseCase {

    private static final String BUCKET_REFERENCES = "references";
    private static final String BUCKET_SUBMISSIONS = "submissions";

    private final AudioStorage audioStorage;
    private final AudioConverter audioConverter;
    private final WorkspaceFactory workspaceFactory;
    private final FeatureExtractor featureExtractor;

    private final PerformanceEvaluator performanceEvaluator;
    private final ReferenceRepository referenceRepository;
    private final ResultRepository resultRepository;
    private final DebugVisualizer debugVisualizer;

    private final AnalysisEventPublisher eventPublisher;

    public void execute(String bucket, String fileKey) {

        if (!BUCKET_REFERENCES.equals(bucket) && !BUCKET_SUBMISSIONS.equals(bucket)) {
            log.warn("Неизвестный бакет: {}. Пропускаем", bucket);
            return;
        }

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

            log.info("Извлечено {} кадров признаков", features.frames().size());

            watch.start("Evaluation & Persistence");
            if (BUCKET_REFERENCES.equals(bucket)) {
                handleTeacherReference(entityId, features);
            } else {
                handleStudentSubmission(entityId, features);
            }
            watch.stop();

            log.info("Результаты обработки {}: \n{}", fileKey, watch.prettyPrint());

        } catch (AnalysisFatalException e) {
            log.error("Fatal analysis error for file {}: {}", fileKey, e.getMessage(), e);

            updateStatus(bucket, entityId, AudioProcessingStatus.FAILED, e.getMessage());
            eventPublisher.publish(AnalysisFinishedEvent.builder()
                    .targetId(entityId)
                    .type(BUCKET_REFERENCES.equals(bucket) ? AnalysisType.REFERENCE : AnalysisType.SUBMISSION)
                    .status(AudioProcessingStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build());
        } catch (Exception e) {

            String errorMsg =
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            log.error("Technical error for file {}: {}\n Retrying...", fileKey, errorMsg, e);

            updateStatus(bucket, entityId, AudioProcessingStatus.FAILED, errorMsg);

            throw new RuntimeException("Technical failure, retrying...", e); // Хотелось бы сюда
            // DLT и не ходить в базу при каждом ретрае
        }
    }

    private void updateStatus(String bucket, UUID id, AudioProcessingStatus status, String error) {
        try {
            if (BUCKET_REFERENCES.equals(bucket)) {
                referenceRepository.updateStatus(id, status, error);
            } else {
                resultRepository.updateStatus(id, status, error);
            }
        } catch (Exception ex) {
            log.error("Failed to update status in DB for {}: {}", id, ex.getMessage());
        }
    }

    private void handleTeacherReference(UUID assignmentId, FeatureSequence teacherFeatures) {
        log.info("Сохраняем извлеченные признаки учителя для задания: {}", assignmentId);
        referenceRepository.save(assignmentId, teacherFeatures);

        eventPublisher.publish(AnalysisFinishedEvent.builder()
                .targetId(assignmentId)
                .type(AnalysisType.REFERENCE)
                .status(AudioProcessingStatus.COMPLETED)
                .build());
    }

    private void handleStudentSubmission(UUID submissionId, FeatureSequence studentFeatures) {
        log.info("Evaluating student submission: {}", submissionId);

        UUID assignmentId = resultRepository
                .findAssignmentIdBySubmissionId(submissionId)
                .orElseThrow(() ->
                        new AnalysisFatalException("Submission " + submissionId + " is not linked to any assignment"));

        FeatureSequence teacherFeatures = referenceRepository
                .findFeaturesById(assignmentId)
                .orElseThrow(() -> new AnalysisFatalException(
                        "Teacher reference features not found for assignment: " + assignmentId));

        AnalysisResult result = performanceEvaluator.evaluate(teacherFeatures, studentFeatures);

        resultRepository.save(submissionId, result);

        eventPublisher.publish(AnalysisFinishedEvent.builder()
                .targetId(submissionId)
                .type(AnalysisType.SUBMISSION)
                .status(AudioProcessingStatus.COMPLETED)
                .totalScore(result.totalScore())
                .build());

        try {
            debugVisualizer.generateHeatmap(result, "debug_" + submissionId + ".png");
        } catch (Exception e) {
            log.error("Не удалось сгенерировать тепловую карту {}: {}", submissionId, e.getMessage());
        }

        log.info("Закончена обработка попытки: {}", submissionId);
    }

    private UUID extractUuid(String fileKey) {
        try {
            String fileName = fileKey.contains("/") ? fileKey.substring(fileKey.lastIndexOf('/') + 1) : fileKey;

            String rawUuid = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

            return UUID.fromString(rawUuid);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid S3 file key. Expected UUID, got: " + fileKey);
        }
    }
}
