package com.banking.core.controller;

import com.banking.core.dto.*;
import com.banking.core.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST para operaciones bancarias (depósito, retiro, transferencia).
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /** Deposita fondos en una cuenta */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(transactionService.deposit(request));
    }

    /** Retira fondos de una cuenta */
    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionResponse> withdrawal(@Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(transactionService.withdrawal(request));
    }

    /**
     * Inicia una transferencia.
     * Si targetBankCode es el banco actual → interna.
     * Si es diferente → saliente vía Kafka.
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.transfer(request));
    }

    /** Historial de transacciones de una cuenta */
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getHistory(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getHistory(accountNumber));
    }
}
