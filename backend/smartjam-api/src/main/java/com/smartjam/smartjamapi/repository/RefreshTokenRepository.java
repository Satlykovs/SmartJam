package com.smartjam.smartjamapi.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.StatusRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String token);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.status = :status  WHERE r.tokenHash = :tokenHash")
    void setStatusByRefreshToken(@Param("tokenHash") String tokenHash, @Param("status") StatusRefreshToken status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE RefreshTokenEntity r
    SET r.status = :newStatus
    WHERE r.user = :user
      AND r.status = :currentStatus
""")
    void updateStatusByUserAndCurrentStatus(
            @Param("user") UserEntity userEntity,
            @Param("currentStatus") StatusRefreshToken currentStatus,
            @Param("newStatus") StatusRefreshToken newStatus);
}
