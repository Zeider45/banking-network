package com.banking.switch_.kafka;

import com.banking.switch_.dto.TransferMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica mensajes de transferencia en el topic del banco destino.
 * Cada banco tiene su propio topic de entrada: "bank.{bankCode}.incoming"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwitchProducer {

    private final KafkaTemplate<String, TransferMessage> kafkaTemplate;

    /**
     * Enruta un mensaje al topic específico del banco destino.
     *
     * @param targetTopic Topic del banco destino (ej: "bank.BANKB.incoming")
     * @param message     Mensaje de transferencia a enrutar
     */
    public void routeToBank(String targetTopic, TransferMessage message) {
        log.info("Enrutando txId={} → topic={}", message.getTransactionId(), targetTopic);

        kafkaTemplate.send(targetTopic, message.getTransactionId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error enrutando txId={} a {}: {}",
                                message.getTransactionId(), targetTopic, ex.getMessage());
                    } else {
                        log.debug("Mensaje enrutado: txId={}, offset={}",
                                message.getTransactionId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
