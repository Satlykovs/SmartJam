package com.smartjam.smartjamapi.service;

import java.security.SecureRandom;
import java.util.*;

import jakarta.persistence.EntityNotFoundException;

import com.smartjam.api.model.*;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.ConnectionsStatus;
import com.smartjam.smartjamapi.exception.CannotJoinSelfException;
import com.smartjam.smartjamapi.exception.ConnectionAlreadyActiveException;
import com.smartjam.smartjamapi.mapper.PageableMapper;
import com.smartjam.smartjamapi.repository.ConnectionsRepository;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConnectionsService {

    private final ConnectionsRepository repository;
    private final IdentityService identityService;
    private final UserRepository userRepository;
    private final PageableMapper pageableMapper;

    @Transactional
    public InviteResponse createInvite() {

        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }

        String inviteCode = sb.toString();

        UUID userId = identityService.getCurrentUserId();
        UserEntity teacher = userRepository.getReferenceById(userId);

        ConnectionsEntity connection = new ConnectionsEntity();
        connection.setTeacher(teacher);
        connection.setInviteCode(inviteCode);
        repository.save(connection);

        return new InviteResponse(inviteCode);
    }

    @Transactional
    public void joinTeacher(JoinRequest request) {
        ConnectionsEntity connection = repository
                .findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new EntityNotFoundException("Invite code not found"));

        if (connection.getStatus() == ConnectionsStatus.ACTIVE) {
            throw new ConnectionAlreadyActiveException("Cannot join, connection is already active");
        }

        UUID userId = identityService.getCurrentUserId();

        if (userId.equals(connection.getTeacher().getId())) {
            throw new CannotJoinSelfException("Teacher cannot join their own connection");
        }

        UserEntity student = userRepository.getReferenceById(userId);
        connection.setStudent(student);
        connection.setStatus(ConnectionsStatus.ACTIVE);
        connection.setInviteCode(null);

        repository.save(connection);
    }

    @Transactional
    public ConnectionPageResponse getMyConnections(Integer page, Integer size, String sort) {

        UUID userId = identityService.getCurrentUserId();
        Set<String> authorities = identityService.getCurrentUserRoles();

        boolean isTeacher = authorities.contains("ROLE_TEACHER");

        Pageable pageable = pageableMapper.toPageable(page, size, sort);

        Page<ConnectionsEntity> pageConnection = isTeacher
                ? repository.findAllByTeacherIdAndStatus(userId, ConnectionsStatus.ACTIVE, pageable)
                : repository.findAllByStudentIdAndStatus(userId, ConnectionsStatus.ACTIVE, pageable);

        PageInfo pageInfo = new PageInfo(
                pageConnection.getTotalElements(),
                pageConnection.getTotalPages(),
                pageConnection.getNumber(),
                pageConnection.getSize());

        List<ConnectionResponse> responses = pageConnection.stream()
                .map(entity -> {
                    UserEntity peer =
                            entity.getTeacher().getId().equals(userId) ? entity.getStudent() : entity.getTeacher();

                    return new ConnectionResponse(
                            entity.getId(),
                            peer.getId(),
                            peer.getUsername(),
                            peer.getFirstName(),
                            peer.getLastName(),
                            peer.getAvatarUrl(),
                            entity.getCreatedAt());
                })
                .toList();

        return new ConnectionPageResponse(responses, pageInfo);
    }

    public ConnectionsEntity getConnectionsEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Connection not found"));
    }
}
