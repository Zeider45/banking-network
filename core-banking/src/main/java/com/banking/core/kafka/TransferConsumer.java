package com.banking.core.kafka;

import com.banking.core.dto.TransferMessage;
import com.banking.core.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Escucha el topic Kafka específico de este banco para recibir transferencias
 * enrutadas desde el Banking Switch.
 *
 * El topic se construye como "bank.{BANK_CODE}.incoming" (ej: "bank.BANKA.incoming").
 * Cada instancia del core-banking escucha solo su propio topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferConsumer {

    private final TransactionService transactionService;

    /**
     * Listener del topic de transferencias entrantes.
     *
     * groupId usa el bankCode para que cada banco tenga su propio consumer group
     * y no compitan entre sí por los mensajes.
     *
     * El topic se configura dinámicamente vía SpEL desde application.yml.
     */
    @KafkaListener(
            topics = "${banking.kafka.topics.incoming}",
            groupId = "${banking.bank-code}-consumer-group",
            containerFactory = "transferKafkaListenerContainerFactory"
    )
    public void onIncomingTransfer(
            @Payload TransferMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Transferencia entrante recibida: txId={}, desde={}, topic={}, partition={}, offset={}",
                message.getTransactionId(), message.getSourceBankCode(), topic, partition, offset);

        try {
            transactionService.receiveIncomingTransfer(message);
        } catch (Exception e) {
            // Log el error pero no relanza: Spring Kafka reintentará según la política configurada
            log.error("Error procesando transferencia entrante: txId={}, error={}",
                    message.getTransactionId(), e.getMessage(), e);
        }
    }
}
