package com.banking.core.domain;

/**
 * Tipos de operación bancaria que puede registrar el core.
 */
public enum TransactionType {
    DEPOSIT,           // Depósito de dinero en cuenta
    WITHDRAWAL,        // Retiro de dinero de cuenta
    TRANSFER_INTERNAL, // Transferencia entre cuentas del mismo banco
    TRANSFER_OUTGOING, // Transferencia saliente a otro banco
    TRANSFER_INCOMING  // Transferencia recibida desde otro banco
}
