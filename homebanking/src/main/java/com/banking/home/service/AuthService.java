package com.banking.home.service;

import com.banking.home.dto.LoginRequest;
import com.banking.home.dto.LoginResponse;
import com.banking.home.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Servicio de autenticación del Home Banking.
 *
 * El Home Banking no tiene base de datos de usuarios propia.
 * Delega la verificación de credenciales al Core Banking del banco
 * correspondiente (identificado por el bankCode en el LoginRequest).
 *
 * Si las credenciales son válidas, genera un JWT para uso posterior.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final WebClient coreBankingClient;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    /**
     * Autentica al usuario contra el Core Banking.
     *
     * 1. Llama al Core Banking para buscar el usuario por email
     * 2. Verifica la contraseña con BCrypt
     * 3. Recupera la cuenta principal del usuario
     * 4. Genera el token JWT con email, bankCode y accountNumber
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Intento de login: email={}, banco={}", request.getEmail(), request.getBankCode());

        // Llama al Core Banking para verificar el usuario
        // El Core Banking expone /api/auth/verify para validar credenciales
        Map<?, ?> userInfo;
        try {
            userInfo = coreBankingClient.post()
                    .uri("/api/auth/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "email", request.getEmail(),
                            "password", request.getPassword()
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Login fallido para {}: {}", request.getEmail(), e.getStatusCode());
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        if (userInfo == null) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        String accountNumber = (String) userInfo.get("accountNumber");
        String fullName = (String) userInfo.get("fullName");

        // Genera token JWT válido por expirationMs
        String token = jwtUtil.generateToken(request.getEmail(), request.getBankCode(), accountNumber);

        log.info("Login exitoso: email={}, banco={}, cuenta={}", request.getEmail(), request.getBankCode(), accountNumber);

        return LoginResponse.builder()
                .token(token)
                .email(request.getEmail())
                .fullName(fullName)
                .bankCode(request.getBankCode())
                .accountNumber(accountNumber)
                .expiresIn(expirationMs)
                .build();
    }
}
