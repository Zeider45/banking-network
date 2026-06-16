package com.banking.home.service;

import com.banking.home.dto.DepositRequest;
import com.banking.home.dto.TransferRequest;
import com.banking.home.dto.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Servicio del Home Banking que orquesta las operaciones bancarias.
 *
 * Actúa como API Gateway: recibe las solicitudes del usuario autenticado
 * y las delega al Core Banking, añadiendo el contexto del usuario
 * (cuenta origen, banco) extraído del JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankingService {

    private final WebClient coreBankingClient;
    private final WebClient switchClient;

    // ─────────────────────────────────────────────────────────────────────────
    // Consultas
    // ─────────────────────────────────────────────────────────────────────────

    /** Consulta el saldo y datos de la cuenta del usuario autenticado */
    public Map<?, ?> getAccount(String accountNumber) {
        log.info("Consultando cuenta: {}", accountNumber);
        return coreBankingClient.get()
                .uri("/api/accounts/{accountNumber}", accountNumber)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /** Historial de movimientos de la cuenta del usuario */
    public Object getHistory(String accountNumber) {
        log.info("Consultando historial: {}", accountNumber);
        return coreBankingClient.get()
                .uri("/api/transactions/history/{accountNumber}", accountNumber)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    /** Lista los bancos disponibles en la red (desde el Banking Switch) */
    public Object getAvailableBanks() {
        return switchClient.get()
                .uri("/api/switch/banks")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Operaciones
    // ─────────────────────────────────────────────────────────────────────────

    /** Deposita fondos en la cuenta del usuario autenticado */
    public Map<?, ?> deposit(String accountNumber, DepositRequest request) {
        log.info("Depósito: cuenta={}, monto={}", accountNumber, request.getAmount());

        Map<String, Object> body = Map.of(
                "accountNumber", accountNumber,
                "amount", request.getAmount(),
                "description", request.getDescription() != null ? request.getDescription() : "Depósito"
        );

        return coreBankingClient.post()
                .uri("/api/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /** Retira fondos de la cuenta del usuario autenticado */
    public Map<?, ?> withdrawal(String accountNumber, WithdrawalRequest request) {
        log.info("Retiro: cuenta={}, monto={}", accountNumber, request.getAmount());

        Map<String, Object> body = Map.of(
                "accountNumber", accountNumber,
                "amount", request.getAmount(),
                "description", request.getDescription() != null ? request.getDescription() : "Retiro"
        );

        return coreBankingClient.post()
                .uri("/api/transactions/withdrawal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * Transfiere fondos desde la cuenta del usuario.
     *
     * Si targetBankCode == banco actual → el Core Banking lo procesa internamente.
     * Si targetBankCode != banco actual → el Core Banking lo publica en Kafka
     *   y el Banking Switch lo enruta al banco destino.
     */
    public Map<?, ?> transfer(String sourceAccountNumber, String sourceBankCode, TransferRequest request) {
        log.info("Transferencia: origen={}, destino={}/{}, monto={}",
                sourceAccountNumber, request.getTargetBankCode(),
                request.getTargetAccountNumber(), request.getAmount());

        Map<String, Object> body = Map.of(
                "sourceAccountNumber", sourceAccountNumber,
                "targetAccountNumber", request.getTargetAccountNumber(),
                "targetBankCode", request.getTargetBankCode(),
                "amount", request.getAmount(),
                "description", request.getDescription() != null ? request.getDescription() : "Transferencia"
        );

        return coreBankingClient.post()
                .uri("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
