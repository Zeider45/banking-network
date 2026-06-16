package com.banking.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro inmutable de una operación bancaria.
 *
 * Toda operación (depósito, retiro, transferencia) genera una transacción
 * que queda persistida para auditoría. Las transacciones nunca se borran,
 * solo se marcan como REVERSED si hay una devolución.
 */
@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID único global de la transacción (UUID) para rastrear entre bancos */
    @Column(nullable = false, unique = true)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /** Monto de la operación */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Cuenta origen de la operación (null en depósitos externos) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    /** Número de cuenta destino (puede estar en otro banco) */
    @Column
    private String targetAccountNumber;

    /** Código del banco destino (para transferencias entre bancos) */
    @Column
    private String targetBankCode;

    /** Descripción o referencia de la operación */
    @Column
    private String description;

    /** Fecha y hora del registro de la transacción */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Fecha y hora en que se completó o falló */
    @Column
    private LocalDateTime processedAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
