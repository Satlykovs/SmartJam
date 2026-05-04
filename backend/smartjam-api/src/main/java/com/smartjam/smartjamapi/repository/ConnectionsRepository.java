package com.smartjam.smartjamapi.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;

import com.smartjam.api.model.ConnectionResponse;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.enums.ConnectionsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ConnectionsRepository extends JpaRepository<ConnectionsEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConnectionsEntity> findByInviteCode(@NotNull String s);

    List<ConnectionResponse> findAllById(UUID userId);

    Page<ConnectionsEntity> findAllByStudentIdAndStatus(UUID studentId, ConnectionsStatus status, Pageable pageable);

    Page<ConnectionsEntity> findAllByTeacherIdAndStatus(UUID teacherId, ConnectionsStatus status, Pageable pageable);
}
