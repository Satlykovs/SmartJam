package com.smartjam.smartjamapi.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.RefreshTokenStatus;
import com.smartjam.smartjamapi.exception.TokenNotFoundException;
import com.smartjam.smartjamapi.repository.RefreshTokenRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Getter
    @Value("${security.jwt.expiration-time-refresh}")
    private long refreshExpiration;

    // TODO: перенести проверку истечения токена и по ситуации ещё что-то

    public String generateRefreshToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashRefreshToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public RefreshTokenEntity create(UserEntity userEntity, String refreshToken) {
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setTokenHash(hashRefreshToken(refreshToken));
        refreshTokenEntity.setUser(userEntity);
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(getRefreshExpiration()));
        refreshTokenEntity.setStatus(RefreshTokenStatus.ACTIVE);

        return refreshTokenEntity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeToken(String tokenHash) {
        repository.setStatusByTokenHash(tokenHash, RefreshTokenStatus.INACTIVE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllToken(UserEntity user) {
        repository.updateStatusByUserAndCurrentStatus(user, RefreshTokenStatus.INACTIVE);
    }

    public void save(RefreshTokenEntity refreshTokenEntity) {
        repository.save(refreshTokenEntity);
    }

    @Transactional
    public RefreshTokenEntity findByTokenHash(String tokenHash) {
        return repository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token not found, try login, please"));
    }
}
