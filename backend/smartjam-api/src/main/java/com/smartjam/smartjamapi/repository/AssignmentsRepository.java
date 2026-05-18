package com.smartjam.smartjamapi.repository;

import java.util.UUID;

import com.smartjam.smartjamapi.entity.AssignmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentsRepository extends JpaRepository<AssignmentEntity, UUID> {

    Page<AssignmentEntity> findByConnectionId(UUID connectionId, Pageable pageable);
}
