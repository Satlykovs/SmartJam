package com.smartjam.smartjamapi.repository;

import java.util.Optional;

import com.smartjam.smartjamapi.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String login);
}
