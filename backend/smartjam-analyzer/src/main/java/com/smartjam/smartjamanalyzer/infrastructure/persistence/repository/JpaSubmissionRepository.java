package com.smartjam.smartjamanalyzer.infrastructure.persistence.repository;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link SubmissionEntity}. Manages persistence for student attempts and their
 * corresponding analysis metrics.
 */
public interface JpaSubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    /**
     * Updates the status and error message of a submission using a partial update query. *
     *
     * @param id Unique identifier of the submission.
     * @param status The new processing status to be set.
     * @param error Error message if the status is FAILED, otherwise null.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SubmissionEntity s SET s.status = :status, s.errorMessage = :error WHERE s.id " + "= :id")
    void updateStatus(UUID id, AudioProcessingStatus status, String error);
}
