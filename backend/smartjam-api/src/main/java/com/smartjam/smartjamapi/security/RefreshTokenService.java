package com.smartjam.smartjamapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import com.smartjam.smartjamapi.entity.RefreshTokenEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.enums.StatusRefreshToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RefreshTokenService {

    @Getter
    @Value("${security.jwt.expiration-time-refresh}")
    private long refreshExpiration;

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
        refreshTokenEntity.setStatus(StatusRefreshToken.ACTIVE);

        return refreshTokenEntity;
    }
}
