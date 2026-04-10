package com.smartjam.smartjamanalyzer.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.ReferenceRepository;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.AssignmentEntity;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.repository.JpaAssignmentRepository;
import com.smartjam.smartjamanalyzer.infrastructure.utils.FeatureBinarySerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link ReferenceRepository}. Bridges the domain logic with the database using binary
 * serialization for spectral data.
 */
@Component
@RequiredArgsConstructor
public class AssignmentPersistenceAdapter implements ReferenceRepository {
    private final JpaAssignmentRepository repository;

    /**
     * Packs spectral features into a binary format and saves them to the assignment record. Transition the status to
     * {@link AudioProcessingStatus#COMPLETED}.
     */
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

    /**
     * Retrieves and unpacks binary features from the database.
     *
     * @return an Optional containing the FeatureSequence, or empty if not found.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<FeatureSequence> findFeaturesById(UUID assignmentId) {
        return repository
                .findById(assignmentId)
                .map(e -> FeatureBinarySerializer.deserialize(e.getReferenceSpectreCache()));
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, AudioProcessingStatus status, String errorMessage) {
        repository.updateStatus(id, status, errorMessage);
    }
}
