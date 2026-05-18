package com.smartjam.smartjamapi.service;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.api.model.*;
import com.smartjam.smartjamapi.entity.AssignmentEntity;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.mapper.PageableMapper;
import com.smartjam.smartjamapi.repository.AssignmentsRepository;
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
public class AssignmentsService {

    private final ConnectionsService connectionsService;
    private final AssignmentsRepository repository;
    private final IdentityService identityService;
    private final PageableMapper pageableMapper;
    private final S3Service s3Service;

    @Transactional
    public AssignmentUploadResponse createAssignment(CreateAssignmentRequest request) {

        ConnectionsEntity connection = connectionsService.getConnectionsEntityById(request.connectionId());

        UUID userId = identityService.getCurrentUserId();
        if (!connection.getTeacher().getId().equals(userId)) {
            throw new AccessDeniedException("Only the connection teacher can create assignments");
        }

        AssignmentEntity newAssignment = AssignmentEntity.builder()
                .connection(connection)
                .title(request.title())
                .description(request.description())
                .s3ReferenceKey(null)
                .build();

        AssignmentEntity saved = repository.save(newAssignment);
        UUID assignmentId = saved.getId();
        String s3Key = s3Service.getAssignmentKey(request.connectionId(), assignmentId);

        saved.setS3ReferenceKey(s3Key);
        repository.save(saved);

        String presignedUrl = s3Service.generatePresignedUrlForTeacher(s3Key);

        return new AssignmentUploadResponse(assignmentId, presignedUrl);
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDetailed getAssignment(UUID assignmentId) {
        AssignmentEntity entity = repository
                .findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));

        ConnectionsEntity connection = entity.getConnection();
        checkConnectionMembership(connection);

        return new AssignmentResponseDetailed(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                s3Service.generatePresignedUrlForDownload(entity.getS3ReferenceKey()));
    }

    @Transactional(readOnly = true)
    public AssignmentPageResponse getAssignmentsByConnection(
            UUID connectionId, Integer page, Integer size, String sort) {

        ConnectionsEntity connection = connectionsService.getConnectionsEntityById(connectionId);
        checkConnectionMembership(connection);

        Pageable pageable = pageableMapper.toPageable(page, size, sort);

        Page<AssignmentEntity> pageAssignment = repository.findByConnectionId(connectionId, pageable);

        PageInfo pageInfo = new PageInfo(
                pageAssignment.getTotalElements(),
                pageAssignment.getTotalPages(),
                pageAssignment.getNumber(),
                pageAssignment.getSize());

        List<AssignmentResponse> responses = pageAssignment.stream()
                .map(entity -> new AssignmentResponse(
                        entity.getId(), entity.getTitle(), entity.getStatus(), entity.getCreatedAt()))
                .toList();

        return new AssignmentPageResponse(responses, pageInfo);
    }

    public void checkConnectionMembership(ConnectionsEntity connection) {
        UUID userId = identityService.getCurrentUserId();
        boolean isTeacher = connection.getTeacher().getId().equals(userId);
        boolean isStudent = connection.getStudent() != null
                && connection.getStudent().getId().equals(userId);

        if (!isTeacher && !isStudent) {
            throw new AccessDeniedException("You are not a member of this connection");
        }
    }

    public AssignmentEntity getAssignmentEntityById(UUID assignmentId) {
        return repository.findById(assignmentId).orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
    }
}
