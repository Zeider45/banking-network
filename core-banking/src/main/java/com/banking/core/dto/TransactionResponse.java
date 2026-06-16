package com.banking.core.dto;

import com.banking.core.domain.TransactionStatus;
import com.banking.core.domain.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vista pública de una transacción */
@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private String targetBankCode;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
