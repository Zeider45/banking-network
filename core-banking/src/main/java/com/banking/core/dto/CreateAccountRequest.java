package com.banking.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Solicitud para abrir una nueva cuenta bancaria */
@Data
public class CreateAccountRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    /** Moneda de la cuenta (USD por defecto) */
    @NotBlank(message = "La moneda es obligatoria")
    private String currency;
}
