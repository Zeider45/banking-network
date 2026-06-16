package com.banking.home.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Principal del usuario autenticado.
 * Almacena email, banco y cuenta principal para evitar llamadas
 * adicionales al Core Banking en cada request.
 */
@Getter
@AllArgsConstructor
public class BankingUserPrincipal {
    private final String email;
    private final String bankCode;
    private final String accountNumber;
}
