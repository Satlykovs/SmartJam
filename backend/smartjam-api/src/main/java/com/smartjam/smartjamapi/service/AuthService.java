package com.smartjam.smartjamapi.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import com.smartjam.smartjamapi.enums.StatusRefreshToken;
import com.smartjam.smartjamapi.exception.TokenExpiredException;
import com.smartjam.smartjamapi.exception.TokenNotFoundException;
import com.smartjam.smartjamapi.mapper.UserMapper;
import com.smartjam.smartjamapi.security.RefreshTokenService;

import com.smartjam.smartjamapi.dto.AuthResponse;
import com.smartjam.smartjamapi.dto.LoginRequest;
import com.smartjam.smartjamapi.dto.RefreshTokenRequest;
import com.smartjam.smartjamapi.dto.RegisterRequest;
import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.Role;
import com.smartjam.smartjamapi.repository.RefreshTokenRepository;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.JwtService;
import com.smartjam.smartjamapi.security.UserDetailsImpl;
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

        UserDetailsImpl userDetails = UserDetailsImpl.build(userEntity);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, refreshToken);

        refreshTokenRepository.setStatusUsedRefreshToken(userEntity, StatusRefreshToken.ACTIVE, StatusRefreshToken.USED);

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Login successful");

        return new AuthResponse(refreshToken, accessToken);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (repository.existsByEmail(request.email())) {
            throw new IllegalStateException("The account exists, try login, please");
        }

        UserEntity userEntity = userMapper.toEntity(request);

        userEntity.setPasswordHash(passwordEncoder.encode(request.password()));
        userEntity.setRole(Role.STUDENT);

        repository.save(userEntity);

        UserDetailsImpl userDetails = UserDetailsImpl.build(userEntity);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, refreshToken);

        refreshTokenRepository.setStatusUsedRefreshToken(userEntity, StatusRefreshToken.ACTIVE, StatusRefreshToken.USED);

        refreshTokenRepository.save(refreshTokenEntity);

        log.info("Register successful");

        return new AuthResponse(refreshToken, accessToken);
    }


    @Transactional
    protected void revokeToken(String tokenHash) {
        refreshTokenRepository.setStatusByRefreshToken(tokenHash, StatusRefreshToken.REVOKED);

        refreshTokenRepository.flush();
    }

    @Transactional
    public AuthResponse getNewTokens(RefreshTokenRequest refreshTokenRequest) {

        String tokenHash = RefreshTokenService.hashRefreshToken(refreshTokenRequest.refreshToken());

        log.info(tokenHash);

        RefreshTokenEntity refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token not found, try login, please"));

        if (refreshToken.getStatus() == StatusRefreshToken.USED) {
            revokeToken(tokenHash);
            log.error(tokenHash);

            throw new SecurityException("Token reuse detected");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            revokeToken(tokenHash);
            log.error(tokenHash);

            throw new TokenExpiredException("Refresh token expired");
        }

        refreshTokenRepository.setStatusByRefreshToken(tokenHash, StatusRefreshToken.USED);
        refreshTokenRepository.flush();

        UserEntity userEntity = refreshToken.getUser();
        UserDetailsImpl userDetails = UserDetailsImpl.build(userEntity);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = refreshTokenService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.create(userEntity, newRefreshToken);
        refreshTokenRepository.save(refreshTokenEntity);


        log.info("New tokens successfully created");

        return new AuthResponse(newRefreshToken, accessToken);
    }
}
