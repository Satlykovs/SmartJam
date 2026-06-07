package com.smartjam.smartjamapi.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.smartjam.api.model.CommentPageResponse;
import com.smartjam.api.model.CommentRequest;
import com.smartjam.api.model.CommentResponse;
import com.smartjam.smartjamapi.entity.AssignmentEntity;
import com.smartjam.smartjamapi.entity.CommentEntity;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.mapper.CommentMapper;
import com.smartjam.smartjamapi.repository.CommentsRepository;
import com.smartjam.smartjamapi.security.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final AssignmentsService assignmentsService;
    private final IdentityService identityService;
    private final CommentsRepository repository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse createComment(UUID assignmentId, CommentRequest request) {

        UUID currentUserId = identityService.getCurrentUserId();

        AssignmentEntity assignment = assignmentsService.getAssignmentEntityById(assignmentId);
        ConnectionsEntity connection = assignment.getConnection();

        UserEntity author;

        boolean isStudent = connection.getStudent().getId().equals(currentUserId);
        boolean isTeacher = connection.getTeacher().getId().equals(currentUserId);

        if (isStudent) {
            author = connection.getStudent();
        } else if (isTeacher) {
            author = connection.getTeacher();
        } else {
            log.warn(
                    "Access denied: User {} tried to CREATE a comment, but is not part of connection {}",
                    currentUserId,
                    connection.getId());
            throw new AccessDeniedException("Only the connection member can create comment to the assignments");
        }

        CommentEntity comment = CommentEntity.builder()
                .text(request.text())
                .assignment(assignment)
                .author(author)
                .build();

        CommentEntity saved = repository.save(comment);

        return commentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse getComments(UUID assignmentId, Integer limit, String cursor) {

        log.info("getComments: limit = {}", limit);

        UUID currentUserId = identityService.getCurrentUserId();

        AssignmentEntity assignment = assignmentsService.getAssignmentEntityById(assignmentId);
        ConnectionsEntity connection = assignment.getConnection();

        boolean isStudent = connection.getStudent().getId().equals(currentUserId);
        boolean isTeacher = connection.getTeacher().getId().equals(currentUserId);

        if (!isStudent && !isTeacher) {
            log.warn(
                    "Access denied: User {} tried to READ comments, but is not part of connection {}",
                    currentUserId,
                    connection.getId());
            throw new AccessDeniedException("Only the connection member can get comments from the assignments");
        }

        Pageable pageable = PageRequest.of(0, limit + 1);

        List<CommentEntity> comments;

        if (cursor == null || cursor.isBlank()) {
            comments = new ArrayList<>(repository.getFirstPage(assignmentId, pageable));
        } else {
            try {
                String[] detCursor = cursor.split(",");
                Instant cursorTime = Instant.parse(detCursor[0]);
                UUID cursorId = UUID.fromString(detCursor[1]);
                comments = new ArrayList<>(repository.getNextPage(assignmentId, cursorTime, cursorId, pageable));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid cursor format");
            }
        }

        log.info("current cursor: {}", cursor);

        boolean hasNext = false;
        String nextCursor = null;
        if (comments.size() == limit + 1) {
            hasNext = true;
            comments.removeLast();
            CommentEntity lastComment = comments.getLast();
            nextCursor = lastComment.getCreatedAt() + "," + lastComment.getId();
        }

        List<CommentResponse> commentsResponses = commentMapper.toResponseList(comments);

        log.info("size commentsResponses: {}", commentsResponses.size());
        return new CommentPageResponse(commentsResponses, nextCursor, hasNext);
    }
}
