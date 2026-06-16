package com.banking.core.domain;

/**
 * Estado del ciclo de vida de una transacción.
 *
 * PENDING   → La transacción fue iniciada pero no confirmada (ej: esperando respuesta del otro banco)
 * COMPLETED → Operación aplicada exitosamente
 * FAILED    → La operación no pudo completarse (saldo insuficiente, cuenta inactiva, etc.)
 * REVERSED  → La transacción fue revertida después de completarse
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
