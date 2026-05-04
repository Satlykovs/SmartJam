package com.smartjam.smartjamapi.controller;

import com.smartjam.api.api.AuthApi;
import com.smartjam.api.model.AuthResponse;
import com.smartjam.api.model.LoginRequest;
import com.smartjam.api.model.RefreshRequest;
import com.smartjam.api.model.RegisterRequest;
import com.smartjam.smartjamapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> registerUser(RegisterRequest body) {
        log.info("Calling register");
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(body));
    }

    @Override
    public ResponseEntity<AuthResponse> loginUser(LoginRequest body) {
        log.info("Calling login");
        return ResponseEntity.ok(authService.login(body));
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshRequest body) {
        log.info("Calling getNewToken");
        return ResponseEntity.ok(authService.refreshToken(body));
    }
}
