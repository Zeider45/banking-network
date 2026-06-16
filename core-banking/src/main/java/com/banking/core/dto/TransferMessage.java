package com.banking.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje que viaja por Kafka para coordinar transferencias entre bancos.
 *
 * El Banking Switch recibe este mensaje desde el banco origen,
 * lo enruta al topic del banco destino, y el banco destino lo consume
 * para acreditar el monto en la cuenta correspondiente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMessage {

    /** UUID único de la transacción para idempotencia */
    private String transactionId;

    /** Banco que origina la transferencia (ej: "BANKA") */
    private String sourceBankCode;

    /** Número de cuenta que envía */
    private String sourceAccountNumber;

    /** Banco destino de la transferencia (ej: "BANKB") */
    private String targetBankCode;

    /** Número de cuenta que recibe */
    private String targetAccountNumber;

    private BigDecimal amount;
    private String currency;
    private String description;

    /** Momento en que se originó la transferencia */
    private LocalDateTime timestamp;

    /** Estado actual del mensaje (INITIATED, COMPLETED, FAILED) */
    private String status;
}
