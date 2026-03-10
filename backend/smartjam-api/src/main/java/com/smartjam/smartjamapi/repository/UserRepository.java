package com.smartjam.smartjamapi.repository;

import com.smartjam.smartjamapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String login);
    Optional<UserEntity> findUserEntitiesByUsername(String username);

}
