package com.smartjam.smartjamanalyzer.infrastructure.persistence.adapter;

import java.util.UUID;

import jakarta.transaction.Transactional;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.ReferenceRepository;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.AssignmentEntity;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.repository.JpaAssignmentRepository;
import com.smartjam.smartjamanalyzer.infrastructure.utils.FeatureBinarySerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentPersistenceAdapter implements ReferenceRepository {
    private final JpaAssignmentRepository repository;

    @Override
    @Transactional
    public void save(UUID assignmentId, FeatureSequence features) {
        byte[] bytes = FeatureBinarySerializer.serialize(features);

        AssignmentEntity entity = repository
                .findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("Assignment metadata missing for ID: " + assignmentId
                        + ". It might have " + "been deleted or not created yet."));

        entity.setReferenceSpectreCache(bytes);
        entity.setStatus(AudioProcessingStatus.COMPLETED);
        entity.setErrorMessage(null);

        repository.save(entity);
    }

    @Override
    public FeatureSequence findById(UUID assignmentId) {
        return repository
                .findById(assignmentId)
                .map(e -> FeatureBinarySerializer.deserialize(e.getReferenceSpectreCache()))
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, AudioProcessingStatus status, String errorMessage) {
        repository.updateStatus(id, status, errorMessage);
    }
}
