package com.smartjam.smartjamanalyzer.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.common.model.AudioProcessingStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Database model for teacher assignments. Stores heavy spectral data as raw bytes (BYTEA) to optimize performance. */
@Entity
@Table(name = "assignments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private AudioProcessingStatus status;

    @Column(name = "reference_spectre_cache")
    private byte[] referenceSpectreCache;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
