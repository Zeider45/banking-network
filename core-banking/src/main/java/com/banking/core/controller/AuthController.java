package com.banking.core.controller;

import com.banking.core.domain.Account;
import com.banking.core.domain.User;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint interno de autenticación del Core Banking.
 *
 * Es llamado por el Home Banking para verificar credenciales de usuario.
 * No genera tokens JWT (eso lo hace el Home Banking), solo verifica
 * que el email y contraseña sean correctos y devuelve los datos del usuario.
 *
 * NOTA: Este endpoint está en red interna Docker, no expuesto al exterior.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Verifica credenciales y devuelve información del usuario.
     * Llamado por el Home Banking durante el proceso de login.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCredentials(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Contraseña incorrecta para usuario: {}", email);
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }

        // Busca la primera cuenta activa del usuario
        Account account = accountRepository.findByOwnerId(user.getId())
                .stream()
                .filter(Account::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene cuentas activas"));

        log.info("Verificación exitosa: {}", email);

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "accountNumber", account.getAccountNumber(),
                "bankCode", account.getBankCode()
        ));
    }
}
