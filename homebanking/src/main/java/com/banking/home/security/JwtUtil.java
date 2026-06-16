package com.banking.home.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utilidad para generación y validación de tokens JWT.
 *
 * El token contiene:
 *  - subject: email del usuario
 *  - bankCode: banco al que pertenece
 *  - accountNumber: cuenta principal del usuario
 *  - expiración: configurable (por defecto 24h)
 */
@Component
@Slf4j
public class JwtUtil {

    /** Clave secreta en Base64 (mínimo 32 bytes para HS256) */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Duración del token en milisegundos */
    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    /** Construye la clave HMAC a partir del secret Base64 */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Genera un token JWT para el usuario autenticado */
    public String generateToken(String email, String bankCode, String accountNumber) {
        return Jwts.builder()
                .subject(email)
                .claim("bankCode", bankCode)
                .claim("accountNumber", accountNumber)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /** Extrae el email (subject) del token */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extrae el código del banco del token */
    public String extractBankCode(String token) {
        return parseClaims(token).get("bankCode", String.class);
    }

    /** Extrae el número de cuenta del token */
    public String extractAccountNumber(String token) {
        return parseClaims(token).get("accountNumber", String.class);
    }

    /** Valida que el token sea correcto y no esté expirado */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
