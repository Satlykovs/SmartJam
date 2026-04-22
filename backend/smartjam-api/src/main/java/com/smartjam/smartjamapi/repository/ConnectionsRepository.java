package com.smartjam.smartjamapi.repository;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.smartjam.api.model.ConnectionResponse;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.enums.ConnectionsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionsRepository extends JpaRepository<ConnectionsEntity, UUID> {
    ConnectionsEntity findByInviteCode(@NotNull String s);

    List<ConnectionResponse> findAllById(UUID userId);

    Page<ConnectionsEntity> findAllByStudentIdAndStatus(UUID student_id, ConnectionsStatus status, Pageable pageable);

    Page<ConnectionsEntity> findAllByTeacherIdAndStatus(UUID teacher_id, ConnectionsStatus status, Pageable pageable);
}
