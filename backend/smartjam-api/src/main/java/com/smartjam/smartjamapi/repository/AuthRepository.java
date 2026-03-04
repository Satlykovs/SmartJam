package com.smartjam.smartjamapi.repository;

import com.smartjam.smartjamapi.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String login);

}
