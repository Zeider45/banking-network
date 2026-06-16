package com.banking.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Respuesta de autenticación exitosa con el token JWT */
@Data
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String fullName;
    private String bankCode;
    private String accountNumber;
    private long expiresIn; // milisegundos
}
