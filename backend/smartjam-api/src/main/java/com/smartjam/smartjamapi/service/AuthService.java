package com.smartjam.smartjamapi.service;

import java.time.Instant;
import java.util.Set;

import com.smartjam.api.model.*;
import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.RefreshTokenStatus;
import com.smartjam.smartjamapi.exception.TokenExpiredException;
import com.smartjam.smartjamapi.exception.TokenReuseException;
import com.smartjam.smartjamapi.mapper.UserMapper;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.jwt.JwtService;
import com.smartjam.smartjamapi.security.jwt.RefreshTokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    private final UserMapper userMapper;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity userEntity = repository
                .findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken =
                jwtService.generateAccessToken(userEntity, UserRole.STUDENT); // INFO: возможно надо будет поменять
        String refreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, refreshToken);

        refreshTokenService.revokeAllToken(userEntity);

        refreshTokenService.save(refreshTokenEntity);

        log.info("Login successful");

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (repository.existsByEmail(request.email()) || repository.existsByUsername(request.username())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserEntity userEntity = userMapper.toEntity(request);

        userEntity.setRoles(Set.of(UserRole.STUDENT, UserRole.TEACHER)); // INFO: возможно надо будет поменять

        userEntity.setPasswordHash(passwordEncoder.encode(request.password()));

        repository.save(userEntity);

        String accessToken =
                jwtService.generateAccessToken(userEntity, UserRole.STUDENT); // INFO: возможно надо будет поменять
        String refreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, refreshToken);

        refreshTokenService.revokeAllToken(userEntity);
        refreshTokenService.save(refreshTokenEntity);

        log.info("Register successful");

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest refreshRequest) {

        String tokenHash = RefreshTokenService.hashRefreshToken(refreshRequest.refreshToken());

        log.debug(tokenHash);

        RefreshTokenEntity refreshToken = refreshTokenService.findByTokenHash(tokenHash);

        if (refreshToken.getStatus() == RefreshTokenStatus.INACTIVE) {
            refreshTokenService.revokeAllToken(refreshToken.getUser());
            throw new TokenReuseException("Token reuse detected");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenService.revokeToken(tokenHash);

            throw new TokenExpiredException("Refresh token expired");
        }

        if (!refreshToken.getUser().getRoles().contains(refreshRequest.asRole())) {
            refreshTokenService.revokeToken(tokenHash);
            log.error(refreshToken.getUser().getRoles().toString());
            throw new AccessDeniedException("User haven't this role");
        }

        refreshTokenService.revokeToken(tokenHash);

        UserEntity userEntity = refreshToken.getUser();

        String accessToken = jwtService.generateAccessToken(userEntity, refreshRequest.asRole());
        String newRefreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, newRefreshToken);
        refreshTokenService.save(refreshTokenEntity);

        log.info("New tokens successfully created");

        return new AuthResponse(accessToken, newRefreshToken);
    }
}
