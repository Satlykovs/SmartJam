package com.smartjam.smartjamanalyzer.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.common.model.AudioProcessingStatus;
import lombok.*;

@Entity
@Table(name = "assignments")
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

    @Column(name = "error_message")
    private String errorMessage;
}
