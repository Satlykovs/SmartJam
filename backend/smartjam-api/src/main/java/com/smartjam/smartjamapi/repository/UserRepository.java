package com.smartjam.smartjamapi.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link UserEntity} persistence operations.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}, as well as derived query methods for
 * email-based lookups used during authentication and registration flows.
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Finds a user by their email address.
     *
     * <p>
     *
     * @param email the email address to search for; must not be {@code null}
     * @return an {@link Optional} containing the matching {@link UserEntity}, or {@link Optional#empty()} if no user
     *     with the given email exists
     * @throws IllegalArgumentException if {@code email} is {@code null}
     * @throws org.springframework.dao.DataAccessException if a database access error occurs (e.g. connection failure,
     *     query execution error); this is a Spring-translated unchecked exception wrapping the underlying
     *     {@link jakarta.persistence.PersistenceException}
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Checks whether a user with the given email address already exists.
     *
     * <p>Prefer this method over {@link #findByEmail(String)} when only presence needs to be verified, as it avoids
     * loading the full entity.
     *
     * <p>
     *
     * @param email the email address to check; must not be {@code null}
     * @return {@code true} if a user with the specified email exists, {@code false} otherwise
     * @throws IllegalArgumentException if {@code email} is {@code null}
     * @throws org.springframework.dao.DataAccessException if a database access error occurs (e.g. connection failure,
     *     query execution error)
     */
    boolean existsByEmail(String email);
}
