package com.banking.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuenta bancaria dentro del core.
 *
 * El número de cuenta es único globalmente: incluye el código del banco
 * para distinguir cuentas entre distintas instancias (ej: BANKA-0001234).
 */
@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de cuenta único (prefijo = código del banco) */
    @Column(nullable = false, unique = true)
    private String accountNumber;

    /** Código del banco al que pertenece esta cuenta (ej: "BANKA") */
    @Column(nullable = false)
    private String bankCode;

    /** Saldo disponible. Nunca puede ser negativo */
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /** Moneda de la cuenta (ej: USD, VES) */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    /** Si la cuenta puede operar (activo/bloqueado) */
    @Builder.Default
    private boolean active = true;

    /** Fecha de apertura */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Dueño de la cuenta */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /** Historial de transacciones de esta cuenta */
    @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
