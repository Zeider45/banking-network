package com.banking.core.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vista pública de una cuenta (sin datos sensibles) */
@Data
@Builder
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String bankCode;
    private BigDecimal balance;
    private String currency;
    private boolean active;
    private String ownerName;
    private LocalDateTime createdAt;
}
