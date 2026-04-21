package com.smartjam.smartjamapi.security.jwt;

import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

import com.smartjam.api.model.UserRole;
import com.smartjam.smartjamapi.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Getter
    @Value("${security.jwt.expiration-time-access}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserEntity user, UserRole activeRole) {
        Map<String, Object> claims = Map.of("asRole", activeRole.name());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
