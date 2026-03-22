package com.smartjam.smartjamanalyzer.infrastructure.persistence.repository;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Spring Data JPA repository for {@link AssignmentEntity}. Includes query for status management. */
public interface JpaAssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {

    /**
     * Performs an update on the record status and error message without loading large binary spectral data into memory.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssignmentEntity a SET a.status = :status, a.errorMessage = :error WHERE a.id " + "= :id")
    void updateStatus(UUID id, AudioProcessingStatus status, String error);
}
