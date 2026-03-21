package com.smartjam.smartjamapi.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
