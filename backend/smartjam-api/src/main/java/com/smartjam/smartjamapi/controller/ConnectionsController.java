package com.smartjam.smartjamapi.controller;

import com.smartjam.api.api.ConnectionsApi;
import com.smartjam.api.model.ConnectionPageResponse;
import com.smartjam.api.model.InviteResponse;
import com.smartjam.api.model.JoinRequest;
import com.smartjam.smartjamapi.service.ConnectionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ConnectionsController implements ConnectionsApi {

    private final ConnectionsService connectionsService;

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<InviteResponse> createInvite() {
        log.info("Calling createInvite");
        return ResponseEntity.status(HttpStatus.CREATED).body(connectionsService.createInvite());
    }

    @Override
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> joinTeacher(JoinRequest body) {
        log.info("Calling joinTeacher");
        connectionsService.joinTeacher(body);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<ConnectionPageResponse> getMyConnections(Integer page, Integer size, String sort) {
        log.info("Calling getMyConnections");
        return ResponseEntity.status(HttpStatus.OK).body(connectionsService.getMyConnections(page, size, sort));
    }
}
