package com.smartjam.smartjamanalyzer.domain.port;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.AnalysisResult;

/**
 * Port for managing student submissions and their analysis results. Handles the persistence of evaluation scores and
 * detailed feedback events.
 */
public interface ResultRepository {

    /**
     * Persists the final analysis result for a specific submission.
     *
     * @param submissionId unique identifier of the student submission.
     * @param result calculated scores and feedback events.
     */
    void save(UUID submissionId, AnalysisResult result);

    /**
     * Resolves the teacher's assignment ID associated with a given student submission.
     *
     * @param submissionId unique identifier of the submission.
     * @return the UUID of the parent assignment.
     * @throws RuntimeException if no linked assignment ID found.
     */
    UUID findAssignmentIdBySubmissionId(UUID submissionId);

    /**
     * Update status for a submission, optionally recording an error message.
     *
     * @param submissionId unique identifier of the submission.
     * @param status the new processing state.
     * @param errorMessage description of the failure, if applicable.
     */
    void updateStatus(UUID submissionId, AudioProcessingStatus status, String errorMessage);
}
