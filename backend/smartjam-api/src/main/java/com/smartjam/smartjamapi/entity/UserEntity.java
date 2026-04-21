package com.smartjam.smartjamapi.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

import com.smartjam.api.model.UserRole;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity representing a registered user in the SmartJam platform.
 *
 * <p>Mapped to the {@code users} database table. The primary key is a UUID generated automatically by the persistence
 * provider. Equality and hash-code are based solely on {@link #id} to ensure correct behavior in JPA-managed
 * collections and during entity detachment/reattachment cycles.
 *
 * <p>The {@code email} field is the unique login identifier. {@code username} is a unique display name. Passwords are
 * never stored in plain text — only the hashed value is persisted in {@code passwordHash}.
 *
 * <p><b>Persistence exceptions:</b> Attempting to persist or merge an instance with a duplicate {@code email}, a
 * {@code null} required field, or a value that violates a column constraint will cause the underlying JPA provider to
 * throw a {@link jakarta.persistence.PersistenceException} (typically wrapped by Spring as
 * {@link org.springframework.dao.DataIntegrityViolationException}).
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Entity
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity {

    /** Unique identifier for the user, generated automatically as a UUID. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /** Unique username of the user shown in the UI. Must not be {@code null}. */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * The user's email address, used as the unique login identifier. Must be a valid email format, non-null, and unique
     * across all users.
     */
    @Column(nullable = false, unique = true)
    @Email
    private String email;

    /** Bcrypt (or equivalent) hash of the user's password. The plain-text password is never stored. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Optional first name of the user. */
    @Column(name = "first_name")
    private String firstName;

    /** Optional last name of the user. */
    @Column(name = "last_name")
    private String lastName;

    /** Optional URL pointing to the user's avatar image. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Set of user Roles {@link UserRole} */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new HashSet<>();

    /** Optional Firebase Cloud Messaging token used to send push notifications to the user's device. */
    @Column(name = "fcm_token")
    private String fcmToken;

    /**
     * Timestamp of when the user record was first created. Set automatically by Hibernate on insert and never updated
     * afterward.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the most recent update to the user record. Updated automatically by Hibernate on every merge/flush.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
