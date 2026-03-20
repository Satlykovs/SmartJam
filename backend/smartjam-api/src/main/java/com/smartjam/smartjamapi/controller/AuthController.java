package com.smartjam.smartjamapi.controller;

import jakarta.validation.Valid;

import com.smartjam.smartjamapi.dto.AuthResponse;
import com.smartjam.smartjamapi.dto.LoginRequest;
import com.smartjam.smartjamapi.dto.RefreshTokenRequest;
import com.smartjam.smartjamapi.dto.RegisterRequest;
import com.smartjam.smartjamapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("Calling login");
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        log.info("Calling register");
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> getNewTokens(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        log.info("Calling getNewToken");
        return ResponseEntity.ok(authService.getNewTokens(refreshTokenRequest));
    }
}
