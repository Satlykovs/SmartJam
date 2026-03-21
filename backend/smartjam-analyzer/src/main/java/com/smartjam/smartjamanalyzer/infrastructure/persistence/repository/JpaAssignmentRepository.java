package com.smartjam.smartjamanalyzer.infrastructure.persistence.repository;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JpaAssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AssignmentEntity a SET a.status = :status, a.errorMessage = :error WHERE a.id " + "= :id")
    void updateStatus(UUID id, AudioProcessingStatus status, String error);
}
