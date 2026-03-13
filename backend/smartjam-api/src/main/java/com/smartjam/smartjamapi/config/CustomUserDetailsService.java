package com.smartjam.smartjamapi.config;

import jakarta.annotation.Nullable;

import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetailsImpl loadUserByUsername(@Nullable String email) throws UsernameNotFoundException {
        UserEntity user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return UserDetailsImpl.build(user);
    }
}
