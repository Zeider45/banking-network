package com.banking.home.controller;

import com.banking.home.dto.LoginRequest;
import com.banking.home.dto.LoginResponse;
import com.banking.home.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos de autenticación.
 * No requieren JWT (son el punto de entrada del sistema).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login: recibe email + password + bankCode, devuelve JWT.
     * El bankCode indica en qué banco buscar al usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
