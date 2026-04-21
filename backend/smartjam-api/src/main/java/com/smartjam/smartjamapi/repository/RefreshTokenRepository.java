package com.smartjam.smartjamapi.repository;

import java.util.Optional;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.RefreshTokenStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for {@link RefreshTokenEntity} persistence operations.
 *
 * <p>In addition to standard CRUD operations inherited from {@link JpaRepository}, this repository provides token-hash
 * lookups and bulk JPQL {@code UPDATE} statements for efficient status transitions without loading full entity graphs.
 *
 * <p>All {@link Modifying} methods use {@code clearAutomatically = true} and {@code flushAutomatically = true} to
 * ensure the persistence context is flushed before and cleared after each bulk update, preventing stale first-level
 * cache entries from being read after the operation.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /**
     * Finds a refresh token record by the hash of the raw token string.
     *
     * <p>
     *
     * @param tokenHash the SHA-256 hex digest (or equivalent hash) of the refresh token; must not be {@code null}
     * @return an {@link Optional} containing the matching {@link RefreshTokenEntity}, or {@link Optional#empty()} if no
     *     record with the given hash exists
     * @throws IllegalArgumentException if {@code token} is {@code null}
     * @throws DataAccessException if a database access error occurs (e.g. connection failure, query execution error)
     */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Updates the status of the refresh token identified by the given token hash.
     *
     * <p>This is a bulk JPQL {@code UPDATE} that does not load the entity into the persistence context, making it more
     * efficient than a find-then-save pattern.
     *
     * <p>
     *
     * @param tokenHash the SHA-256 hex digest of the refresh token whose status should be updated; must not be
     *     {@code null}
     * @param status the new {@link RefreshTokenStatus} to set; must not be {@code null}
     * @return the number of rows affected by the update (0 or 1)
     * @throws IllegalArgumentException if {@code tokenHash} or {@code status} is {@code null}
     * @throws org.springframework.dao.DataAccessException if a database access error occurs during the update
     * @throws jakarta.persistence.TransactionRequiredException if called outside an active transaction (normally
     *     prevented by the {@code @Transactional} annotation on this method)
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.status = :status WHERE r.tokenHash = :tokenHash")
    int setStatusByTokenHash(@Param("tokenHash") String tokenHash, @Param("status") RefreshTokenStatus status);

    /**
     * Bulk-updates the status of all refresh tokens belonging to the given user whose current status matches
     * {@code currentStatus}.
     *
     * <p>Typically used to invalidate all active tokens for a user on logout or password change, by transitioning them
     * from {@link RefreshTokenStatus#ACTIVE} to {@link RefreshTokenStatus#INACTIVE}.
     *
     * <p>
     *
     * @param user the owner {@link UserEntity} whose tokens should be updated; must not be {@code null}
     * @param newStatus the new {@link RefreshTokenStatus} to assign; must not be {@code null}
     * @return the number of rows affected by the update
     * @throws IllegalArgumentException if any of {@code user}, {@code currentStatus}, or {@code newStatus} is
     *     {@code null}
     * @throws org.springframework.dao.DataAccessException if a database access error occurs during the update
     * @throws jakarta.persistence.TransactionRequiredException if called outside an active transaction (normally
     *     prevented by the {@code @Transactional} annotation on this method)
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE RefreshTokenEntity r
    SET r.status = :newStatus
    WHERE r.user = :user
""")
    int updateStatusByUserAndCurrentStatus(
            @Param("user") UserEntity user,
            @Param("newStatus") RefreshTokenStatus newStatus);
}
