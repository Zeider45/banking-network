package com.banking.core.service;

import com.banking.core.domain.Account;
import com.banking.core.domain.User;
import com.banking.core.dto.AccountResponse;
import com.banking.core.dto.CreateAccountRequest;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión de cuentas bancarias.
 * Las operaciones de saldo (depósito, retiro, transferencia) están en TransactionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /** Código del banco inyectado desde variable de entorno (ej: BANKA) */
    @Value("${banking.bank-code}")
    private String bankCode;

    /** Prefijo para generar números de cuenta únicos */
    @Value("${banking.account-prefix:ACC}")
    private String accountPrefix;

    /**
     * Crea una nueva cuenta para el usuario especificado.
     * El número de cuenta se genera automáticamente con el prefijo del banco.
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + request.getUserId()));

        // Genera número de cuenta único: BANKA-ACC-000001
        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .bankCode(bankCode)
                .currency(request.getCurrency())
                .owner(user)
                .build();

        account = accountRepository.save(account);
        log.info("Cuenta creada: {} para usuario: {}", accountNumber, user.getEmail());

        return mapToResponse(account);
    }

    /** Consulta todas las cuentas de un usuario */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(Long userId) {
        return accountRepository.findByOwnerId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Consulta una cuenta por número */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + accountNumber));
        return mapToResponse(account);
    }

    /** Genera un número de cuenta único en formato BANKA-ACC-000001 */
    private String generateAccountNumber() {
        // Usa el total de cuentas + 1 como secuencia simple
        long count = accountRepository.count() + 1;
        String sequence = String.format("%06d", count);
        String candidate = bankCode + "-" + accountPrefix + "-" + sequence;

        // Si hay colisión (raro), incrementa hasta encontrar un número libre
        while (accountRepository.existsByAccountNumber(candidate)) {
            count++;
            sequence = String.format("%06d", count);
            candidate = bankCode + "-" + accountPrefix + "-" + sequence;
        }
        return candidate;
    }

    /** Convierte entidad Account a DTO de respuesta */
    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .bankCode(account.getBankCode())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .active(account.isActive())
                .ownerName(account.getOwner().getFullName())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
