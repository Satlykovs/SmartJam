package com.smartjam.smartjamapi.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.smartjamapi.enums.StatusRefreshToken;
import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusRefreshToken status;
}
