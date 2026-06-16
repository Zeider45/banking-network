package com.banking.core.kafka;

import com.banking.core.dto.TransferMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publica mensajes de transferencia en el topic de Kafka del Banking Switch.
 *
 * El Banking Switch escucha el topic "switch.transfers" y enruta el mensaje
 * al topic específico del banco destino ("bank.{bankCode}.incoming").
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferProducer {

    private final KafkaTemplate<String, TransferMessage> kafkaTemplate;

    /** Topic que escucha el Banking Switch para recibir transferencias */
    @Value("${banking.kafka.topics.outgoing:switch.transfers}")
    private String outgoingTopic;

    /**
     * Envía una transferencia saliente al Banking Switch.
     * La clave del mensaje es el transactionId para garantizar ordering
     * en particiones Kafka (idempotencia por orden).
     */
    public void sendOutgoingTransfer(TransferMessage message) {
        log.info("Publicando transferencia saliente en Kafka: txId={}, topic={}",
                message.getTransactionId(), outgoingTopic);

        CompletableFuture<SendResult<String, TransferMessage>> future =
                kafkaTemplate.send(outgoingTopic, message.getTransactionId(), message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Error al publicar transferencia en Kafka: txId={}, error={}",
                        message.getTransactionId(), ex.getMessage());
            } else {
                log.debug("Transferencia publicada exitosamente: txId={}, offset={}",
                        message.getTransactionId(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
