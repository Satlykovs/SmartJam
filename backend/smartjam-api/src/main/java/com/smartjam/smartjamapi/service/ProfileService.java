package com.smartjam.smartjamapi.service;

import java.util.UUID;

import com.smartjam.api.model.UserAvatarResponse;
import com.smartjam.api.model.UserProfileUpdateRequest;
import com.smartjam.api.model.UserResponse;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.exception.UserAlreadyExistsException;
import com.smartjam.smartjamapi.mapper.UserMapper;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final IdentityService identityService;
    private final UserRepository repository;
    private final UserMapper userMapper;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        UUID userId = identityService.getCurrentUserId();
        // INFO: такого не может быть, надо обдумать, но что стоит кидать или не кидать я не знаю
        UserEntity user = repository
                .findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserAvatarResponse updateCurrentUserProfile(UserProfileUpdateRequest request) {

        String avatarUrl = null;

        UUID userId = identityService.getCurrentUserId();
        UserEntity user = repository
                .findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (request.username() != null
                && !request.username().isBlank()
                && !request.username().equals(user.getUsername())) {
            if (repository.existsByUsername(request.username())) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            user.setUsername(request.username());
        }

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }

        if (request.avatarUpdated() != null && request.avatarUpdated()) {
            String key = s3Service.getTempAvatarsKey(userId);
            avatarUrl = s3Service.generatePresignedUrlForUserAvatar(key);
        }
        return new UserAvatarResponse(avatarUrl);
    }
}
