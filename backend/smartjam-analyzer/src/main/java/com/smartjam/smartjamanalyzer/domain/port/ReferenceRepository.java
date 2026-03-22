package com.smartjam.smartjamanalyzer.domain.port;

import java.util.Optional;
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
     * @return The feature sequence if presented.
     */
    Optional<FeatureSequence> findFeaturesById(UUID assignmentId);

    /**
     * Performs an optimized status update for an assignment, optionally recording an error message.
     *
     * @param assignmentId unique identifier of the submission.
     * @param status the new processing state.
     * @param errorMessage description of the failure, if applicable.
     */
    void updateStatus(UUID assignmentId, AudioProcessingStatus status, String errorMessage);
}
