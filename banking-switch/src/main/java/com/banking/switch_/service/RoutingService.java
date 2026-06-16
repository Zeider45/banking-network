package com.banking.switch_.service;

import com.banking.switch_.dto.BankInfo;
import com.banking.switch_.dto.TransferMessage;
import com.banking.switch_.kafka.SwitchProducer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Lógica de enrutamiento del Banking Switch.
 *
 * Recibe un mensaje de transferencia, valida el banco destino y lo
 * publica en el topic Kafka específico del banco receptor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final BankRegistryService bankRegistry;
    private final SwitchProducer switchProducer;
    private final MeterRegistry meterRegistry;

    /**
     * Enruta una transferencia al banco destino.
     *
     * Flujo:
     *  1. Busca el banco destino en el registro
     *  2. Valida que el banco esté activo
     *  3. Publica el mensaje en el topic del banco destino
     *  4. Si el banco no existe, registra el error (en prod enviaría notificación de rechazo)
     */
    public void route(TransferMessage message) {
        log.info("Switch recibió transferencia: txId={}, origen={}, destino={}",
                message.getTransactionId(), message.getSourceBankCode(), message.getTargetBankCode());

        // Buscar el banco destino en el registro
        Optional<BankInfo> targetBankOpt = bankRegistry.findBank(message.getTargetBankCode());

        if (targetBankOpt.isEmpty()) {
            log.error("Banco destino no registrado: {}. Transferencia rechazada: txId={}",
                    message.getTargetBankCode(), message.getTransactionId());
            // Registra métrica de rechazo
            meterRegistry.counter("switch.routing.rejected",
                    "reason", "bank_not_found",
                    "source_bank", message.getSourceBankCode()).increment();
            return;
        }

        BankInfo targetBank = targetBankOpt.get();

        if (!targetBank.isActive()) {
            log.error("Banco destino inactivo: {}. Transferencia rechazada: txId={}",
                    message.getTargetBankCode(), message.getTransactionId());
            meterRegistry.counter("switch.routing.rejected",
                    "reason", "bank_inactive",
                    "source_bank", message.getSourceBankCode()).increment();
            return;
        }

        // Actualizar estado del mensaje antes de reenviar
        message.setStatus("ROUTED");
        message.setTimestamp(LocalDateTime.now());

        // Publicar en el topic del banco destino
        switchProducer.routeToBank(targetBank.getIncomingTopic(), message);

        // Métricas: contador de transferencias enrutadas por par de bancos
        meterRegistry.counter("switch.routing.success",
                "source_bank", message.getSourceBankCode(),
                "target_bank", message.getTargetBankCode()).increment();

        log.info("Transferencia enrutada exitosamente: txId={} → {} (topic={})",
                message.getTransactionId(), message.getTargetBankCode(), targetBank.getIncomingTopic());
    }
}
