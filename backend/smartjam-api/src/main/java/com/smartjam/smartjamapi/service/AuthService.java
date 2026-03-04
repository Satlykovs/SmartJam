package com.smartjam.smartjamapi.service;

import java.util.NoSuchElementException;

import com.smartjam.smartjamapi.*;
import com.smartjam.smartjamapi.dto.AuthResponse;
import com.smartjam.smartjamapi.dto.LoginRequest;
import com.smartjam.smartjamapi.dto.RegisterRequest;
import com.smartjam.smartjamapi.enums.AvailabilityStatus;
import com.smartjam.smartjamapi.repository.AuthRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity userEntity = repository
                .findByEmail(request.email())
                .orElseThrow(() -> new NoSuchElementException("Login not found, try register, please"));
        if (!passwordEncoder.matches(request.password(), userEntity.getPasswordHash())) {
            throw new IllegalStateException("Invalid password");
        }

        return new AuthResponse("Entrance is allowed", AvailabilityStatus.AVAILABLE);
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
        userEntity.setRole("USER");

        repository.save(userEntity);

        return new AuthResponse("Registration was successful", AvailabilityStatus.AVAILABLE);
    }
}
