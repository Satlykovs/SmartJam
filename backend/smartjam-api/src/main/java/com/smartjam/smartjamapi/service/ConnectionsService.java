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
import com.smartjam.smartjamapi.repository.ConnectionsRepository;
import com.smartjam.smartjamapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConnectionsService {

    private final ConnectionsRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public InviteResponse createInvite() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        String inviteCode = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        UUID userUUID = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName());

        UserEntity teacher = userRepository.getReferenceById(userUUID);

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

        UUID userUUID = UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName());

        if (userUUID.equals(connection.getTeacher().getId())) {
            throw new CannotJoinSelfException("Teacher cannot join their own connection");
        }

        UserEntity student = userRepository.getReferenceById(userUUID);
        connection.setStudent(student);
        connection.setStatus(ConnectionsStatus.ACTIVE);

        repository.save(connection);
    }

    @Transactional
    public ConnectionPageResponse getMyConnections(Integer page, Integer size, String sort) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String[] argsSort = sort.split(",\\s*");
        String field = argsSort[0];
        Sort.Direction direction = Sort.Direction.DESC;

        if (argsSort.length > 1 && argsSort[1].equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, field));

        Page<ConnectionsEntity> pageConnection = authorities.getFirst().equals("ROLE_TEACHER")
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

    public ConnectionsEntity getUUIDConnection(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Connection not found"));
    }
}
