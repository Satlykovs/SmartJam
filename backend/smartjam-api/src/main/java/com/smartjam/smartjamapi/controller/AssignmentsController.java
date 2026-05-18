package com.smartjam.smartjamapi.controller;

import java.util.UUID;

import com.smartjam.api.api.AssignmentsApi;
import com.smartjam.api.model.AssignmentPageResponse;
import com.smartjam.api.model.AssignmentResponseDetailed;
import com.smartjam.api.model.AssignmentUploadResponse;
import com.smartjam.api.model.CreateAssignmentRequest;
import com.smartjam.smartjamapi.service.AssignmentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AssignmentsController implements AssignmentsApi {

    private final AssignmentsService assignmentsService;

    @Override
    public ResponseEntity<AssignmentPageResponse> getAssignmentsByConnection(
            UUID connectionId, Integer page, Integer size, String sort) {
        log.info("Calling getAssignmentsByConnection");
        return ResponseEntity.status(HttpStatus.OK)
                .body(assignmentsService.getAssignmentsByConnection(connectionId, page, size, sort));
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AssignmentUploadResponse> createAssignment(CreateAssignmentRequest body) {
        log.info("Calling createAssignment");
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentsService.createAssignment(body));
    }

    @Override
    public ResponseEntity<AssignmentResponseDetailed> getAssignment(UUID assignmentId) {
        log.info("Calling getAssignment");
        return ResponseEntity.status(HttpStatus.OK).body(assignmentsService.getAssignment(assignmentId));
    }
}
