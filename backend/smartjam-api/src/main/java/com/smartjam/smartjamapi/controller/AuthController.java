package com.smartjam.smartjamapi.controller;

import com.smartjam.smartjamapi.dto.AuthResponse;
import com.smartjam.smartjamapi.dto.LoginRequest;
import com.smartjam.smartjamapi.dto.RegisterRequest;
import com.smartjam.smartjamapi.dto.TokenDto;
import com.smartjam.smartjamapi.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request
    ) {
        log.info("Calling login");
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        log.info("Calling register");
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> getNewToken(
            @RequestBody @Valid TokenDto tokenDto) {
        log.info("Calling getNewToken");
        return ResponseEntity.status(201).body(authService.getNewToken(tokenDto));
    }
}
