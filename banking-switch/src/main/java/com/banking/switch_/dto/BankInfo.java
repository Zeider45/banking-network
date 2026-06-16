package com.banking.switch_.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información de un banco registrado en el Switch.
 * El Switch usa esta información para construir el topic de Kafka destino.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankInfo {
    /** Código único del banco (ej: BANKA, BANKB) */
    private String bankCode;

    /** Nombre descriptivo del banco */
    private String bankName;

    /**
     * Topic de Kafka donde el banco escucha transferencias entrantes.
     * Patrón: "bank.{bankCode}.incoming"
     */
    private String incomingTopic;

    /** Si el banco está disponible para recibir transferencias */
    private boolean active;
}
