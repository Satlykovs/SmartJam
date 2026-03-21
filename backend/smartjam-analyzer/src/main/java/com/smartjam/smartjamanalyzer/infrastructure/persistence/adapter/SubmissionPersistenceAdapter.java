package com.smartjam.smartjamanalyzer.infrastructure.persistence.adapter;

import java.util.UUID;

import jakarta.transaction.Transactional;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;
import com.smartjam.smartjamanalyzer.domain.port.ResultRepository;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.SubmissionEntity;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.repository.JpaSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionPersistenceAdapter implements ResultRepository {
    private final JpaSubmissionRepository repository;

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
    public UUID findAssignmentIdBySubmissionId(UUID submissionId) {
        return repository
                .findById(submissionId)
                .map(SubmissionEntity::getAssignmentId)
                .orElseThrow(() -> new RuntimeException("Submission not linked to any assignment: " + submissionId));
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, AudioProcessingStatus status, String errorMessage) {
        repository.updateStatus(id, status, errorMessage);
    }
}
