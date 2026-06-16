package com.banking.switch_.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje de transferencia que fluye por Kafka entre el Core Banking y el Switch.
 * Debe ser idéntico al TransferMessage del core-banking para compatibilidad de serialización.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMessage {
    private String transactionId;
    private String sourceBankCode;
    private String sourceAccountNumber;
    private String targetBankCode;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime timestamp;
    private String status;
}
