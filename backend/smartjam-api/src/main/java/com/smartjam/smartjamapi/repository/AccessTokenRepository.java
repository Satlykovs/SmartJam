package com.smartjam.smartjamapi.repository;

import java.util.Optional;

import com.smartjam.smartjamapi.entity.AccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTokenRepository extends JpaRepository<AccessTokenEntity, Long> {
    Optional<AccessTokenEntity> findByJti(String jti);
}