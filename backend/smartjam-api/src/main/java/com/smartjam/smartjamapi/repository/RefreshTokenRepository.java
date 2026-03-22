package com.smartjam.smartjamapi.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String token);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.status = :status WHERE r.tokenHash = :tokenHash")
    boolean setStatusByTokenHash(@Param("tokenHash") String tokenHash, @Param("status") RefreshTokenStatus status);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE RefreshTokenEntity r
    SET r.status = :newStatus
    WHERE r.user = :user
      AND r.status = :currentStatus
""")
    boolean updateStatusByUserAndCurrentStatus(
            @Param("user") UserEntity user,
            @Param("currentStatus") RefreshTokenStatus currentStatus,
            @Param("newStatus") RefreshTokenStatus newStatus);
}
