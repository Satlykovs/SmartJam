package com.smartjam.smartjamapi.entity;

import java.util.UUID;

import jakarta.persistence.*;

import com.smartjam.smartjamapi.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Column(name = "fcm_token")
    private String fcmToken;

    //    @Override
    //    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    //        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    //    }
    //
    //    @Override
    //    public @NonNull String getPassword() {
    //        return passwordHash;
    //    }
}
