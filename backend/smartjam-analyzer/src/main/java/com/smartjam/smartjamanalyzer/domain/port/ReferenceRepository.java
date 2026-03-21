package com.smartjam.smartjamanalyzer.domain.port;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;

/** Domain port for managing teacher reference features. */
public interface ReferenceRepository {

    /**
     * Saves the spectral features of a teacher's reference track.
     *
     * @param assignmentId Unique identifier of the assignment.
     * @param features Extracted features to persist.
     */
    void save(UUID assignmentId, FeatureSequence features);

    /**
     * Retrieves reference features for comparison.
     *
     * @param assignmentId Unique identifier of the assignment.
     * @return The feature sequence or null if not found.
     */
    FeatureSequence findById(UUID assignmentId);

    void updateStatus(UUID id, AudioProcessingStatus status, String errorMessage);
}
