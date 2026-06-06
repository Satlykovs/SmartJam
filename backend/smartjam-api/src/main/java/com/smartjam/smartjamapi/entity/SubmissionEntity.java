package com.smartjam.smartjamapi.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.common.model.FeedbackEvent;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = {"assignment", "student"})
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "submissions")
public class SubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssignmentEntity assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(name = "s3_submission_key", length = 500)
    private String s3SubmissionKey;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AudioProcessingStatus status = AudioProcessingStatus.AWAITING_UPLOAD;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "pitch_score")
    private Double pitchScore;

    @Column(name = "rhythm_score")
    private Double rhythmScore;

    @Column(name = "total_score")
    private Double totalScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_feedback", columnDefinition = "jsonb")
    private List<FeedbackEvent> analysisFeedback;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "teacher_waveform", columnDefinition = "real[]")
    private List<Float> teacherWaveform;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "student_waveform", columnDefinition = "real[]")
    private List<Float> studentWaveform;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
