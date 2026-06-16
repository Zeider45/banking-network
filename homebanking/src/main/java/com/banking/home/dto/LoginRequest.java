package com.banking.home.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Credenciales de acceso al Home Banking */
@Data
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /** Código del banco donde el usuario tiene su cuenta (ej: BANKA) */
    @NotBlank(message = "El código del banco es obligatorio")
    private String bankCode;
}
