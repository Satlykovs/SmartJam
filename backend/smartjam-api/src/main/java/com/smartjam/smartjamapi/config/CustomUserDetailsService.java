package com.smartjam.smartjamapi.config;

import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.UserDetailsImpl;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetailsImpl loadUserByUsername(@NotBlank String email) throws UsernameNotFoundException {
        UserEntity user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return UserDetailsImpl.build(user);
    }
}
