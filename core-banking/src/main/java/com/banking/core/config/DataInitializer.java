package com.banking.core.config;

import com.banking.core.domain.Account;
import com.banking.core.domain.User;
import com.banking.core.repository.AccountRepository;
import com.banking.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Carga datos iniciales de prueba al arrancar el servicio.
 * Solo se ejecuta si la base de datos está vacía.
 *
 * Crea dos usuarios con sus cuentas para poder probar transferencias
 * sin configurar nada manualmente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${banking.bank-code}")
    private String bankCode;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Base de datos ya tiene datos, saltando inicialización");
            return;
        }

        log.info("Inicializando datos de prueba para banco: {}", bankCode);

        // Usuario 1: cliente estándar
        User user1 = userRepository.save(User.builder()
                .fullName("Juan García")
                .email("juan@" + bankCode.toLowerCase() + ".com")
                .password(passwordEncoder.encode("password123"))
                .role("ROLE_USER")
                .build());

        Account account1 = accountRepository.save(Account.builder()
                .accountNumber(bankCode + "-ACC-000001")
                .bankCode(bankCode)
                .balance(new BigDecimal("10000.00"))
                .currency("USD")
                .owner(user1)
                .build());

        // Usuario 2: otro cliente
        User user2 = userRepository.save(User.builder()
                .fullName("María López")
                .email("maria@" + bankCode.toLowerCase() + ".com")
                .password(passwordEncoder.encode("password123"))
                .role("ROLE_USER")
                .build());

        Account account2 = accountRepository.save(Account.builder()
                .accountNumber(bankCode + "-ACC-000002")
                .bankCode(bankCode)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .owner(user2)
                .build());

        log.info("Datos iniciales creados:");
        log.info("  Usuario: {} | Cuenta: {} | Saldo: {}",
                user1.getEmail(), account1.getAccountNumber(), account1.getBalance());
        log.info("  Usuario: {} | Cuenta: {} | Saldo: {}",
                user2.getEmail(), account2.getAccountNumber(), account2.getBalance());
    }
}
