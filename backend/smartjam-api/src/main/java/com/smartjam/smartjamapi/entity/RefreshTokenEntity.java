package com.smartjam.smartjamapi.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.smartjamapi.enums.RefreshTokenStatus;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA entity representing a refresh token record in the SmartJam platform.
 *
 * <p>Mapped to the {@code refresh_tokens} database table. Rather than storing the raw token string, only a
 * cryptographic hash of the token is persisted in {@link #tokenHash}. This prevents token replay attacks in the event
 * of a database compromise.
 *
 * <p>The lifecycle of a token is tracked via the {@link #status} field. Tokens start as
 * {@link RefreshTokenStatus#ACTIVE} and transition to {@link RefreshTokenStatus#INACTIVE} upon revocation or expiry.
 *
 * <p><b>Persistence exceptions:</b> Attempting to persist or merge an instance with a duplicate {@code tokenHash}, a
 * {@code null} required field, or a value that violates a column constraint will cause the JPA provider to throw a
 * {@link jakarta.persistence.PersistenceException} (typically wrapped by Spring as
 * {@link org.springframework.dao.DataIntegrityViolationException}).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity {

    /** Unique identifier for the refresh token record, generated automatically as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Cryptographic hash (e.g. SHA-256 hex digest) of the opaque refresh token string. Stored instead of the raw token
     * to mitigate replay attacks from a compromised database. Length is 64 characters, sufficient for a SHA-256 hex
     * digest.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * The user to whom this refresh token belongs. Loaded lazily to avoid unnecessary joins when only token metadata is
     * needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** The point in time at which this refresh token expires and must no longer be accepted. */
    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    /**
     * The current lifecycle status of this refresh token. Defaults to {@link RefreshTokenStatus#ACTIVE} upon creation.
     * Persisted as a {@link String} in the database.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RefreshTokenStatus status = RefreshTokenStatus.ACTIVE;

    /**
     * Timestamp of when this refresh token record was first created. Set automatically by Hibernate on insert and never
     * updated afterwards.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the most recent update to this refresh token record. Updated automatically by Hibernate on every
     * merge/flush.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
