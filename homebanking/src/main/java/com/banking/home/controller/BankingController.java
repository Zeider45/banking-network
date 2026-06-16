package com.banking.home.controller;

import com.banking.home.dto.DepositRequest;
import com.banking.home.dto.TransferRequest;
import com.banking.home.dto.WithdrawalRequest;
import com.banking.home.security.BankingUserPrincipal;
import com.banking.home.service.BankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints del Home Banking protegidos con JWT.
 *
 * Todos los endpoints usan @AuthenticationPrincipal para obtener
 * los datos del usuario autenticado sin consultar la base de datos.
 * Los datos (bankCode, accountNumber) vienen directamente del token JWT.
 */
@RestController
@RequestMapping("/api/banking")
@RequiredArgsConstructor
public class BankingController {

    private final BankingService bankingService;

    /** Consulta el saldo y datos de la cuenta del usuario autenticado */
    @GetMapping("/account")
    public ResponseEntity<?> getAccount(@AuthenticationPrincipal BankingUserPrincipal user) {
        return ResponseEntity.ok(bankingService.getAccount(user.getAccountNumber()));
    }

    /** Historial de movimientos */
    @GetMapping("/account/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal BankingUserPrincipal user) {
        return ResponseEntity.ok(bankingService.getHistory(user.getAccountNumber()));
    }

    /** Lista los bancos disponibles en la red */
    @GetMapping("/banks")
    public ResponseEntity<?> getAvailableBanks() {
        return ResponseEntity.ok(bankingService.getAvailableBanks());
    }

    /** Deposita fondos en la propia cuenta */
    @PostMapping("/deposit")
    public ResponseEntity<Map<?, ?>> deposit(
            @AuthenticationPrincipal BankingUserPrincipal user,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(bankingService.deposit(user.getAccountNumber(), request));
    }

    /** Retira fondos de la propia cuenta */
    @PostMapping("/withdrawal")
    public ResponseEntity<Map<?, ?>> withdrawal(
            @AuthenticationPrincipal BankingUserPrincipal user,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(bankingService.withdrawal(user.getAccountNumber(), request));
    }

    /**
     * Transfiere fondos a otro usuario (mismo banco o interbancario).
     * La cuenta origen es siempre la del usuario autenticado.
     */
    @PostMapping("/transfer")
    public ResponseEntity<Map<?, ?>> transfer(
            @AuthenticationPrincipal BankingUserPrincipal user,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(
                bankingService.transfer(user.getAccountNumber(), user.getBankCode(), request));
    }
}
