package com.smartjam.smartjamapi.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.smartjam.smartjamapi.entity.CommentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentsRepository extends JpaRepository<CommentEntity, UUID> {
    @Query("""
            SELECT c FROM CommentEntity c
            WHERE c.assignment.id = :assignmentId
            AND (c.createdAt > :cursorTime OR (c.createdAt = :cursorTime AND c.id > :cursorId))
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<CommentEntity> getNextPage(UUID assignmentId, OffsetDateTime cursorTime, UUID cursorId, Pageable pageable);

    @Query("""
            SELECT c FROM CommentEntity c
            WHERE c.assignment.id = :assignmentId
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<CommentEntity> getFirstPage(UUID assignmentId, Pageable pageable);
}
