package com.smartjam.smartjamanalyzer.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.port.ResultRepository;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.SubmissionEntity;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.repository.JpaSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link ResultRepository}. Manages storage of evaluation results and coordinates mapping between
 * domain results and JSONB database columns.
 */
@Component
@RequiredArgsConstructor
public class SubmissionPersistenceAdapter implements ResultRepository {
    private final JpaSubmissionRepository repository;

    /**
     * Updates the submission record with scores and feedback. Transition the status to
     * {@link AudioProcessingStatus#COMPLETED}.
     */
    @Override
    @Transactional
    public void save(UUID submissionId, AnalysisResult result) {
        SubmissionEntity entity = repository
                .findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("Submission record missing for ID: " + submissionId));

        entity.setTotalScore(result.totalScore());
        entity.setPitchScore(result.pitchScore());
        entity.setRhythmScore(result.rhythmScore());
        entity.setAnalysisFeedback(result.feedback());
        entity.setStatus(AudioProcessingStatus.COMPLETED);
        entity.setErrorMessage(null);

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findAssignmentIdBySubmissionId(UUID submissionId) {
        return repository.findById(submissionId).map(SubmissionEntity::getAssignmentId);
    }

    @Override
    @Transactional
    public void updateStatus(UUID submissionId, AudioProcessingStatus status, String errorMessage) {
        repository.updateStatus(submissionId, status, errorMessage);
    }
}
