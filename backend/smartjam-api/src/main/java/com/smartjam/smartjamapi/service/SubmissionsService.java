package com.smartjam.smartjamapi.service;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.api.model.*;
import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamapi.entity.AssignmentEntity;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.entity.SubmissionEntity;
import com.smartjam.smartjamapi.mapper.PageableMapper;
import com.smartjam.smartjamapi.repository.SubmissionsRepository;
import com.smartjam.smartjamapi.security.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionsService {

    private final AssignmentsService assignmentsService;
    private final IdentityService identityService;
    private final SubmissionsRepository repository;
    private final PageableMapper pageableMapper;
    private final S3Service s3Service;

    @Transactional
    public SubmissionUploadResponse createSubmission(UUID assignmentId) {
        UUID userId = identityService.getCurrentUserId();

        AssignmentEntity assignment = assignmentsService.getAssignmentEntityById(assignmentId);
        ConnectionsEntity connection = assignment.getConnection();

        if (connection.getStudent() == null || !connection.getStudent().getId().equals(userId)) {
            throw new AccessDeniedException("Only the student of the connection can create submissions");
        }

        SubmissionEntity newSubmission = SubmissionEntity.builder()
                .assignment(assignment)
                .student(connection.getStudent())
                .s3SubmissionKey(null)
                .status(AudioProcessingStatus.AWAITING_UPLOAD)
                .build();

        SubmissionEntity saved = repository.save(newSubmission);
        UUID submissionId = saved.getId();

        String s3Key = s3Service.getSubmissionKey(assignmentId, submissionId);
        saved.setS3SubmissionKey(s3Key);
        repository.save(saved);

        String presignedUrl = s3Service.generatePresignedUrlForStudent(s3Key);

        return new SubmissionUploadResponse(submissionId, presignedUrl);
    }

    @Transactional(readOnly = true)
    public SubmissionResultResponse getSubmissionResult(UUID submissionId) {
        SubmissionEntity submission = repository
                .findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));

        AssignmentEntity assignment = submission.getAssignment();
        ConnectionsEntity connection = assignment.getConnection();

        assignmentsService.checkConnectionMembership(connection);

        String referenceAudioUrl = null;
        if (assignment.getS3ReferenceKey() != null) {
            referenceAudioUrl = s3Service.generatePresignedUrlForDownload(assignment.getS3ReferenceKey());
        }

        String submissionAudioUrl = null;
        if (submission.getS3SubmissionKey() != null) {
            submissionAudioUrl = s3Service.generatePresignedUrlForDownload(submission.getS3SubmissionKey());
        }

        return new SubmissionResultResponse(
                submission.getId(),
                submission.getStatus(),
                submission.getTotalScore(),
                submission.getPitchScore(),
                submission.getRhythmScore(),
                submission.getErrorMessage(),
                referenceAudioUrl,
                submissionAudioUrl,
                submission.getAnalysisFeedback(),
                submission.getTeacherWaveform(),
                submission.getStudentWaveform());
    }

    @Transactional(readOnly = true)
    public SubmissionPageResponse getSubmissionsByAssignment(
            UUID assignmentId, Integer page, Integer size, String sort) {

        AssignmentEntity assignment = assignmentsService.getAssignmentEntityById(assignmentId);
        ConnectionsEntity connection = assignment.getConnection();

        assignmentsService.checkConnectionMembership(connection);

        Pageable pageable = pageableMapper.toPageable(page, size, sort);

        Page<SubmissionEntity> pageSubmission = repository.findByAssignmentId(assignmentId, pageable);

        PageInfo pageInfo = new PageInfo(
                pageSubmission.getTotalElements(),
                pageSubmission.getTotalPages(),
                pageSubmission.getNumber(),
                pageSubmission.getSize());

        List<SubmissionResponse> responses = pageSubmission.stream()
                .map(s -> new SubmissionResponse(s.getId(), s.getStatus(), s.getTotalScore(), s.getCreatedAt()))
                .toList();

        return new SubmissionPageResponse(responses, pageInfo);
    }
}
