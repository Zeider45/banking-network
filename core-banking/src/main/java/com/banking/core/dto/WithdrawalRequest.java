package com.banking.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/** Solicitud de retiro de fondos de una cuenta */
@Data
public class WithdrawalRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    private String accountNumber;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto mínimo es 0.01")
    private BigDecimal amount;

    private String description;
}
