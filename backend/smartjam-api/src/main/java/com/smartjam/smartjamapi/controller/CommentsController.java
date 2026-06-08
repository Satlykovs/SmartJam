package com.smartjam.smartjamapi.controller;

import java.util.UUID;

import com.smartjam.api.api.CommentsApi;
import com.smartjam.api.model.CommentPageResponse;
import com.smartjam.api.model.CommentRequest;
import com.smartjam.api.model.CommentResponse;
import com.smartjam.smartjamapi.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CommentsController implements CommentsApi {

    private final CommentService commentService;

    @Override
    public ResponseEntity<CommentResponse> createComment(UUID assignmentId, CommentRequest body) {
        log.info("Calling createComment");
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(assignmentId, body));
    }

    @Override
    public ResponseEntity<CommentPageResponse> getComments(UUID assignmentId, Integer limit, String cursor) {
        log.info("Calling getComments");
        return ResponseEntity.status(HttpStatus.OK).body(commentService.getComments(assignmentId, limit, cursor));
    }
}
