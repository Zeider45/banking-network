package com.banking.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/** Solicitud de transferencia (interna o hacia otro banco) */
@Data
public class TransferRequest {

    @NotBlank(message = "La cuenta origen es obligatoria")
    private String sourceAccountNumber;

    @NotBlank(message = "La cuenta destino es obligatoria")
    private String targetAccountNumber;

    /**
     * Código del banco destino.
     * Si es igual al banco actual → transferencia interna.
     * Si es diferente → se enruta por Kafka al Banking Switch.
     */
    @NotBlank(message = "El código del banco destino es obligatorio")
    private String targetBankCode;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto mínimo es 0.01")
    private BigDecimal amount;

    private String description;
}
