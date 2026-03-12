package com.smartjam.smartjamapi.config;

import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.UserDetailsImpl;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetailsImpl loadUserByUsername(@Nullable String email) throws UsernameNotFoundException {
        UserEntity user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return UserDetailsImpl.build(user);
    }
}
