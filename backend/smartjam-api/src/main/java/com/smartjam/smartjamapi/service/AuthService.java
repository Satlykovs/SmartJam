package com.smartjam.smartjamapi.service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Set;

import com.smartjam.api.model.*;
import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.RefreshTokenStatus;
import com.smartjam.smartjamapi.exception.TokenExpiredException;
import com.smartjam.smartjamapi.exception.TokenNotFoundException;
import com.smartjam.smartjamapi.exception.UserAlreadyExistsException;
import com.smartjam.smartjamapi.mapper.UserMapper;
import com.smartjam.smartjamapi.repository.RefreshTokenRepository;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.jwt.JwtService;
import com.smartjam.smartjamapi.security.jwt.RefreshTokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final UserMapper userMapper;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity userEntity = repository
                .findByEmail(request.email())
                .orElseThrow(() -> new NoSuchElementException("Login not found, try register, please"));
        if (!passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new IllegalStateException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(userEntity, UserRole.STUDENT); // INFO: возможно надо будет поменять
        String refreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, refreshToken);

        revokeAllToken(userEntity);

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Login successful");

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("This email is already registered");
        }

        if (repository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("This username is already registered");
        }

        UserEntity userEntity = userMapper.toEntity(request);

        userEntity.setRoles(Set.of(UserRole.STUDENT, UserRole.TEACHER)); // INFO: возможно будет поменяно, но пока сразу две

        userEntity.setPasswordHash(passwordEncoder.encode(request.password()));

        repository.save(userEntity);

        String accessToken = jwtService.generateAccessToken(userEntity, UserRole.STUDENT); // INFO: возможно надо будет поменять
        String refreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, refreshToken);

        revokeAllToken(userEntity);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Register successful");

        return new AuthResponse(accessToken, refreshToken);
    }


    protected void revokeToken(String tokenHash) {
        refreshTokenRepository.setStatusByTokenHash(tokenHash, RefreshTokenStatus.INACTIVE);
    }


    protected void revokeAllToken(UserEntity user) {
        refreshTokenRepository.updateStatusByUserAndCurrentStatus(
                user, RefreshTokenStatus.INACTIVE);
    }

    //TODO: подумай как правильно выбрасывать ошибки(про фильтр до ендпоинтах в секьюрити чейн и в бизнес логике)

    @Transactional
    public AuthResponse refreshToken(RefreshRequest refreshRequest) {

        String tokenHash = RefreshTokenService.hashRefreshToken(refreshRequest.refreshToken());

        log.debug(tokenHash);

        RefreshTokenEntity refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token not found, try login, please"));

        if (refreshToken.getStatus() == RefreshTokenStatus.INACTIVE) {
            revokeAllToken(refreshToken.getUser());
            log.error(tokenHash);

            throw new SecurityException("Token reuse detected");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            revokeToken(tokenHash);
            log.error(tokenHash);

            throw new TokenExpiredException("Refresh token expired");
        }


        if (!refreshToken.getUser().getRoles().contains(refreshRequest.asRole())) {
            revokeToken(tokenHash);
            log.error(refreshToken.getUser().getRoles().toString());
            throw new SecurityException("User haven't this role");
        }

        revokeToken(tokenHash);

        UserEntity userEntity = refreshToken.getUser();

        String accessToken = jwtService.generateAccessToken(userEntity, refreshRequest.asRole());
        String newRefreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, newRefreshToken);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("New tokens successfully created");

        return new AuthResponse(accessToken, newRefreshToken);
    }
}