package com.smartjam.smartjamanalyzer.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.common.dto.FeedbackEvent;
import com.smartjam.common.model.AudioProcessingStatus;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Database model for student submissions. Utilizes Hibernate 6 JSON support to store structured feedback events in a
 * JSONB column.
 */
@Entity
@Table(name = "submissions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEntity {

    @Id
    private UUID id;

    @Column(name = "assignment_id")
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    private AudioProcessingStatus status;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "pitch_score")
    private Double pitchScore;

    @Column(name = "rhythm_score")
    private Double rhythmScore;

    @Column(name = "error_message")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_feedback")
    private List<FeedbackEvent> analysisFeedback;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
