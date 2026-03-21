package com.smartjam.smartjamanalyzer.infrastructure.persistence.repository;

import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.infrastructure.persistence.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JpaSubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SubmissionEntity s SET s.status = :status, s.errorMessage = :error WHERE s.id " + "= :id")
    void updateStatus(UUID id, AudioProcessingStatus status, String error);
}
