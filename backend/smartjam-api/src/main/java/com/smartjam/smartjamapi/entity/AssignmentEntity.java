package com.smartjam.smartjamapi.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.common.model.AudioProcessingStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "assignments")
public class AssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id")
    private ConnectionsEntity connection;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AudioProcessingStatus status = AudioProcessingStatus.AWAITING_UPLOAD;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "s3_reference_key", length = 500)
    private String s3ReferenceKey;

    @Column(name = "reference_spectre_cache", columnDefinition = "BYTEA")
    private byte[] referenceSpectreCache;

    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
