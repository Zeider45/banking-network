package com.banking.switch_.kafka;

import com.banking.switch_.dto.TransferMessage;
import com.banking.switch_.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del Banking Switch.
 *
 * Escucha el topic "switch.transfers" donde TODOS los bancos publican
 * sus transferencias salientes. El Switch las recibe y las enruta.
 *
 * Este consumer usa un único group-id ("switch-consumer-group") para
 * que solo una instancia del Switch procese cada mensaje.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwitchConsumer {

    private final RoutingService routingService;

    @KafkaListener(
            topics = "${banking.kafka.topics.incoming:switch.transfers}",
            groupId = "switch-consumer-group",
            containerFactory = "switchKafkaListenerContainerFactory"
    )
    public void onTransfer(
            @Payload TransferMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Switch recibió mensaje: topic={}, partition={}, offset={}, txId={}",
                topic, partition, offset, message.getTransactionId());

        try {
            routingService.route(message);
        } catch (Exception e) {
            log.error("Error en enrutamiento: txId={}, error={}",
                    message.getTransactionId(), e.getMessage(), e);
        }
    }
}
