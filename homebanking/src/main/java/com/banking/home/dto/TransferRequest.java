package com.banking.home.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/** Solicitud de transferencia desde el Home Banking */
@Data
public class TransferRequest {

    @NotBlank(message = "La cuenta destino es obligatoria")
    private String targetAccountNumber;

    /**
     * Banco destino. Si es el mismo banco del usuario → transferencia interna.
     * Si es diferente → interbancaria vía Banking Switch.
     */
    @NotBlank(message = "El código del banco destino es obligatorio")
    private String targetBankCode;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto mínimo es 0.01")
    private BigDecimal amount;

    private String description;
}
