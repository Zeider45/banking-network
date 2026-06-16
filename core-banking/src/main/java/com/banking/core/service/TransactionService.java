package com.banking.core.service;

import com.banking.core.domain.*;
import com.banking.core.dto.*;
import com.banking.core.kafka.TransferProducer;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio principal de operaciones bancarias.
 *
 * Maneja depósitos, retiros y transferencias (internas e interbancarias).
 * Usa bloqueo pesimista en cuentas para garantizar consistencia bajo concurrencia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferProducer transferProducer;
    private final MeterRegistry meterRegistry; // Para métricas de Prometheus

    @Value("${banking.bank-code}")
    private String bankCode;

    // ─────────────────────────────────────────────────────────────────────────
    // DEPÓSITO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deposita fondos en una cuenta.
     * La cuenta se bloquea durante la transacción para evitar inconsistencias.
     */
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Depósito iniciado: cuenta={}, monto={}", request.getAccountNumber(), request.getAmount());

        // Bloqueo pesimista: ninguna otra operación puede leer/escribir esta cuenta mientras tanto
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + request.getAccountNumber()));

        validateAccountActive(account);

        // Acreditar el saldo
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        // Registrar la transacción
        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .sourceAccount(account)
                .description(request.getDescription())
                .processedAt(LocalDateTime.now())
                .build();

        tx = transactionRepository.save(tx);

        // Registrar métrica en Prometheus
        meterRegistry.counter("banking.transactions", "type", "deposit", "bank", bankCode).increment();

        log.info("Depósito completado: txId={}, cuenta={}, nuevo saldo={}",
                tx.getTransactionId(), account.getAccountNumber(), account.getBalance());

        return mapToResponse(tx);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RETIRO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retira fondos de una cuenta.
     * Valida que haya saldo suficiente antes de debitar.
     */
    @Transactional
    public TransactionResponse withdrawal(WithdrawalRequest request) {
        log.info("Retiro iniciado: cuenta={}, monto={}", request.getAccountNumber(), request.getAmount());

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + request.getAccountNumber()));

        validateAccountActive(account);
        validateSufficientFunds(account, request.getAmount());

        // Debitar el saldo
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .sourceAccount(account)
                .description(request.getDescription())
                .processedAt(LocalDateTime.now())
                .build();

        tx = transactionRepository.save(tx);

        meterRegistry.counter("banking.transactions", "type", "withdrawal", "bank", bankCode).increment();

        log.info("Retiro completado: txId={}, cuenta={}, nuevo saldo={}",
                tx.getTransactionId(), account.getAccountNumber(), account.getBalance());

        return mapToResponse(tx);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRANSFERENCIA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicia una transferencia.
     *
     * Si el banco destino es el mismo → transferencia interna (una sola transacción DB).
     * Si el banco destino es diferente → debita localmente y publica en Kafka para que
     * el Banking Switch la enrute al banco destino.
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Transferencia iniciada: origen={}, destino={}/{}, monto={}",
                request.getSourceAccountNumber(), request.getTargetBankCode(),
                request.getTargetAccountNumber(), request.getAmount());

        Account sourceAccount = accountRepository.findByAccountNumberWithLock(request.getSourceAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no encontrada"));

        validateAccountActive(sourceAccount);
        validateSufficientFunds(sourceAccount, request.getAmount());

        // Determinar si la transferencia es interna o interbancaria
        boolean isInternal = bankCode.equalsIgnoreCase(request.getTargetBankCode());

        if (isInternal) {
            return executeInternalTransfer(sourceAccount, request);
        } else {
            return executeOutgoingTransfer(sourceAccount, request);
        }
    }

    /**
     * Transferencia interna: ambas cuentas están en este mismo banco.
     * Debita origen y acredita destino en una única transacción de base de datos.
     */
    private TransactionResponse executeInternalTransfer(Account sourceAccount, TransferRequest request) {
        Account targetAccount = accountRepository.findByAccountNumberWithLock(request.getTargetAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no encontrada"));

        validateAccountActive(targetAccount);

        // Mover fondos
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .type(TransactionType.TRANSFER_INTERNAL)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .sourceAccount(sourceAccount)
                .targetAccountNumber(request.getTargetAccountNumber())
                .targetBankCode(bankCode)
                .description(request.getDescription())
                .processedAt(LocalDateTime.now())
                .build();

        tx = transactionRepository.save(tx);

        meterRegistry.counter("banking.transactions", "type", "transfer_internal", "bank", bankCode).increment();

        log.info("Transferencia interna completada: txId={}", tx.getTransactionId());
        return mapToResponse(tx);
    }

    /**
     * Transferencia saliente: el banco destino es diferente.
     * Debita la cuenta origen y publica el mensaje en Kafka.
     * El Banking Switch recibirá el mensaje y lo enrutará al banco destino.
     */
    private TransactionResponse executeOutgoingTransfer(Account sourceAccount, TransferRequest request) {
        // Debitar fondos inmediatamente (fondos retenidos hasta confirmar)
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(sourceAccount);

        String txId = UUID.randomUUID().toString();

        // Registrar transacción como PENDING hasta recibir confirmación del banco destino
        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .type(TransactionType.TRANSFER_OUTGOING)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .sourceAccount(sourceAccount)
                .targetAccountNumber(request.getTargetAccountNumber())
                .targetBankCode(request.getTargetBankCode())
                .description(request.getDescription())
                .build();

        tx = transactionRepository.save(tx);

        // Publicar mensaje en Kafka para que el Banking Switch lo enrute
        TransferMessage message = TransferMessage.builder()
                .transactionId(txId)
                .sourceBankCode(bankCode)
                .sourceAccountNumber(sourceAccount.getAccountNumber())
                .targetBankCode(request.getTargetBankCode())
                .targetAccountNumber(request.getTargetAccountNumber())
                .amount(request.getAmount())
                .currency(sourceAccount.getCurrency())
                .description(request.getDescription())
                .timestamp(LocalDateTime.now())
                .status("INITIATED")
                .build();

        transferProducer.sendOutgoingTransfer(message);

        meterRegistry.counter("banking.transactions", "type", "transfer_outgoing", "bank", bankCode).increment();

        log.info("Transferencia saliente iniciada: txId={}, destino banco={}", txId, request.getTargetBankCode());
        return mapToResponse(tx);
    }

    /**
     * Acredita fondos recibidos desde otro banco (vía Kafka).
     * Este método es llamado por el TransferConsumer al recibir un mensaje de Kafka.
     */
    @Transactional
    public void receiveIncomingTransfer(TransferMessage message) {
        log.info("Procesando transferencia entrante: txId={}, origen={}/{}",
                message.getTransactionId(), message.getSourceBankCode(), message.getSourceAccountNumber());

        // Idempotencia: si ya procesamos esta transacción, ignorarla
        if (transactionRepository.findByTransactionId(message.getTransactionId()).isPresent()) {
            log.warn("Transacción duplicada ignorada: {}", message.getTransactionId());
            return;
        }

        Account targetAccount = accountRepository.findByAccountNumberWithLock(message.getTargetAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cuenta destino no encontrada: " + message.getTargetAccountNumber()));

        validateAccountActive(targetAccount);

        // Acreditar fondos
        targetAccount.setBalance(targetAccount.getBalance().add(message.getAmount()));
        accountRepository.save(targetAccount);

        Transaction tx = Transaction.builder()
                .transactionId(message.getTransactionId())
                .type(TransactionType.TRANSFER_INCOMING)
                .status(TransactionStatus.COMPLETED)
                .amount(message.getAmount())
                .sourceAccount(targetAccount) // En ingresantes, mapeamos a la cuenta receptora
                .targetAccountNumber(message.getTargetAccountNumber())
                .targetBankCode(bankCode)
                .description("Recibido desde " + message.getSourceBankCode() + ": " + message.getDescription())
                .processedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);

        meterRegistry.counter("banking.transactions", "type", "transfer_incoming", "bank", bankCode).increment();

        log.info("Transferencia entrante completada: txId={}, cuenta={}, nuevo saldo={}",
                message.getTransactionId(), targetAccount.getAccountNumber(), targetAccount.getBalance());
    }

    /** Obtiene el historial de transacciones de una cuenta */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(String accountNumber) {
        return transactionRepository
                .findBySourceAccountAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validaciones internas
    // ─────────────────────────────────────────────────────────────────────────

    private void validateAccountActive(Account account) {
        if (!account.isActive()) {
            throw new IllegalStateException("La cuenta está inactiva: " + account.getAccountNumber());
        }
    }

    private void validateSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    String.format("Saldo insuficiente. Disponible: %s, Requerido: %s",
                            account.getBalance(), amount));
        }
    }

    private TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionId(tx.getTransactionId())
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .sourceAccountNumber(tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null)
                .targetAccountNumber(tx.getTargetAccountNumber())
                .targetBankCode(tx.getTargetBankCode())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .processedAt(tx.getProcessedAt())
                .build();
    }
}
