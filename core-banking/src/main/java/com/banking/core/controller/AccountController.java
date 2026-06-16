package com.banking.core.controller;

import com.banking.core.dto.AccountResponse;
import com.banking.core.dto.CreateAccountRequest;
import com.banking.core.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST para gestión de cuentas bancarias.
 * Estos endpoints son consumidos principalmente por el Home Banking y el Banking Switch.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** Abre una nueva cuenta para un usuario */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    /** Consulta una cuenta por número */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    /** Lista todas las cuentas de un usuario */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUser(userId));
    }
}
