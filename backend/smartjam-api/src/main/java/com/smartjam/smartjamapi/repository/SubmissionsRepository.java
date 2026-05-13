package com.smartjam.smartjamapi.repository;

import java.util.UUID;

import com.smartjam.smartjamapi.entity.SubmissionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionsRepository extends JpaRepository<SubmissionEntity, UUID> {
    Page<SubmissionEntity> findByAssignmentId(UUID assignmentId, Pageable pageable);
}
