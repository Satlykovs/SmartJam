package com.smartjam.smartjamapi.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import jakarta.transaction.Transactional;

import com.smartjam.smartjamapi.dto.AuthResponse;
import com.smartjam.smartjamapi.dto.LoginRequest;
import com.smartjam.smartjamapi.dto.RegisterRequest;
import com.smartjam.smartjamapi.dto.TokenDto;
import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.AvailabilityStatus;
import com.smartjam.smartjamapi.enums.Role;
import com.smartjam.smartjamapi.repository.RefreshTokenRepository;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.JwtService;
import com.smartjam.smartjamapi.security.UserDetailsImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        UserEntity userEntity = repository
                .findByEmail(request.email())
                .orElseThrow(() -> new NoSuchElementException("Login not found, try register, please"));
        if (!passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new IllegalStateException("Invalid password");
        }

        UserDetailsImpl userDetails = UserDetailsImpl.build(userEntity);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setUser(userEntity);
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(jwtService.getJwtExpiration()));

        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse("Logged in successfully", AvailabilityStatus.AVAILABLE, refreshToken, accessToken);
    }

    public AuthResponse register(RegisterRequest request) {
        boolean exists = repository.findByEmail(request.email()).isPresent();

        if (exists) {
            throw new IllegalStateException("The account exists, try login, please");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.username());
        userEntity.setEmail(request.email());
        userEntity.setPasswordHash(passwordEncoder.encode(request.password()));
        userEntity.setRole(Role.STUDENT);

        repository.save(userEntity);

        UserDetailsImpl userDetails = UserDetailsImpl.build(userEntity);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setUser(userEntity);
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(jwtService.getJwtExpiration()));

        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse("Register successfully", AvailabilityStatus.AVAILABLE, refreshToken, accessToken);
    }

    @Transactional
    public AuthResponse getNewToken(TokenDto tokenDto) {
        RefreshTokenEntity refreshToken = refreshTokenRepository
                .findByToken(tokenDto.refresh_token())
                .orElseThrow(() -> new NoSuchElementException("Token not found, try login, please"));
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Refresh token expired");
        }

        UserEntity userEntity = repository
                .findById(refreshToken.getUser().getId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        UserDetailsImpl userDetails = UserDetailsImpl.build(userEntity);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken();

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setToken(newRefreshToken);
        refreshTokenEntity.setUser(userEntity);
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(jwtService.getJwtExpiration()));

        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(
                "Token generate successfully", AvailabilityStatus.AVAILABLE, newRefreshToken, accessToken);
    }
}
