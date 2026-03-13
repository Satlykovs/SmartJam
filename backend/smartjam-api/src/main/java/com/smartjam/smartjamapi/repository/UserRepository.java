package com.smartjam.smartjamapi.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String login);

    Optional<UserEntity> findById(UUID id);

    Optional<UserEntity> findUserEntitiesByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
