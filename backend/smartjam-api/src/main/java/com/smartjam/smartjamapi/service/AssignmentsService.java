package com.smartjam.smartjamapi.service;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.api.model.*;
import com.smartjam.smartjamapi.entity.AssignmentEntity;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.repository.AssignmentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentsService {

    private final AssignmentsRepository repository;
    private final ConnectionsService connectionsService;
    private final S3Service s3Service;

    @Transactional
    public AssignmentUploadResponse createAssignment(CreateAssignmentRequest request) {

        AssignmentEntity newAssignment = AssignmentEntity.builder()
                .connection(connectionsService.getUUIDConnection(request.connectionId()))
                .title(request.title())
                .description(request.description())
                .s3ReferenceKey(null)
                .build();

        AssignmentEntity saved = repository.save(newAssignment);
        UUID assignmentId = saved.getId();
        String s3Key = s3Service.getKey(request.connectionId(), assignmentId);

        saved.setS3ReferenceKey(s3Key);
        repository.save(saved);

        String presignedUrl = s3Service.generatePresignedUrlForTeacher(s3Key);

        return new AssignmentUploadResponse(assignmentId, presignedUrl);
    }

    public AssignmentResponseDetailed getAssignment(UUID assignmentId) {
        AssignmentEntity entity = repository
                .findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        ConnectionsEntity connection = entity.getConnection();
        if (!connection.getTeacher().getId().equals(userId)
                && (connection.getStudent() == null
                        || !connection.getStudent().getId().equals(userId))) {
            throw new AccessDeniedException("You are not a member of this connection");
        }

        return new AssignmentResponseDetailed(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                s3Service.generatePresignedUrlForDownload(entity.getS3ReferenceKey()));
    }

    @Transactional
    public AssignmentPageResponse getAssignmentsByConnection(
            UUID connectionId, Integer page, Integer size, String sort) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        ConnectionsEntity connection = connectionsService.getUUIDConnection(connectionId);

        if (!connection.getTeacher().getId().equals(userId)
                && (connection.getStudent() == null
                        || !connection.getStudent().getId().equals(userId))) {
            throw new AccessDeniedException("You are not a member of this connection");
        }

        String[] argsSort = sort.split(",\\s*");
        String field = argsSort[0];
        Sort.Direction direction = Sort.Direction.DESC;

        if (argsSort.length > 1 && argsSort[1].equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, field));

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
}
