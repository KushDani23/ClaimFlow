package com.company.icps.authentication.controller;

import com.company.icps.authentication.dto.AuthResponse;
import com.company.icps.authentication.dto.LoginRequest;
import com.company.icps.authentication.dto.RegisterRequest;
import com.company.icps.authentication.service.AuthService;
import com.company.icps.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new user with CUSTOMER role and returns a JWT token")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success("User registered successfully", authResponse, HttpStatus.CREATED.value()),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "Login", description = "Authenticates user credentials and returns a JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}
