package com.smartjam.smartjamapi.controller;

import java.util.UUID;

import com.smartjam.api.api.SubmissionsApi;
import com.smartjam.api.model.SubmissionPageResponse;
import com.smartjam.api.model.SubmissionResultResponse;
import com.smartjam.api.model.SubmissionUploadResponse;
import com.smartjam.smartjamapi.service.SubmissionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SubmissionsController implements SubmissionsApi {

    private final SubmissionsService submissionsService;

    @Override
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionUploadResponse> createSubmission(UUID assignmentId) {
        log.info("Calling createAssignment");
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionsService.createSubmission(assignmentId));
    }

    @Override
    public ResponseEntity<SubmissionPageResponse> getSubmissionsByAssignment(
            UUID assignmentId, Integer page, Integer size, String sort) {
        log.info("Calling getSubmissionsByAssignment");
        return ResponseEntity.status(HttpStatus.OK)
                .body(submissionsService.getSubmissionsByAssignment(assignmentId, page, size, sort));
    }

    @Override
    public ResponseEntity<SubmissionResultResponse> getSubmissionResult(UUID submissionId) {
        log.info("Calling getSubmissionResult");
        return ResponseEntity.status(HttpStatus.OK).body(submissionsService.getSubmissionResult(submissionId));
    }
}
