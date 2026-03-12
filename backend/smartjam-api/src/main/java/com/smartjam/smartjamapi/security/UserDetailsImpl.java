package com.smartjam.smartjamapi.security;

import java.util.Collection;
import java.util.List;

import com.smartjam.smartjamapi.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
@NullMarked
public class UserDetailsImpl implements UserDetails {
    @Getter
    private Long id;

    private String username;

    @Getter
    private String email;

    private String password;

    public static UserDetailsImpl build(UserEntity user) {
        return new UserDetailsImpl(user.getId(), user.getUsername(), user.getEmail(), user.getPassword());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
