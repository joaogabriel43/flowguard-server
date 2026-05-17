package com.flowguard.presentation.controller;

import com.flowguard.application.dto.LoginCommand;
import com.flowguard.application.dto.RegisterCommand;
import com.flowguard.application.dto.TokenResponse;
import com.flowguard.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterCommand command) {
        TokenResponse response = authService.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginCommand command) {
        TokenResponse response = authService.login(command);
        return ResponseEntity.ok(response);
    }
}
