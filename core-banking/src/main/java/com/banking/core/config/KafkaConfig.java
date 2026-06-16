package com.banking.core.config;

import com.banking.core.dto.TransferMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de Kafka para el Core Banking.
 *
 * Define:
 *  - ProducerFactory: serializa mensajes JSON hacia el Banking Switch
 *  - ConsumerFactory: deserializa mensajes JSON del Banking Switch
 *  - KafkaListenerContainerFactory: con ACK manual para control preciso de offsets
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${banking.bank-code}")
    private String bankCode;

    // ─────────────────────────────────────────────────────────────────────────
    // PRODUCER
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, TransferMessage> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // NO enviar el header __TypeId__ con el FQCN de la clase Java.
        // Si se envía, el consumer de otro módulo (banking-switch) falla al intentar
        // resolver com.banking.core.dto.TransferMessage en su classpath.
        // Con false, cada consumer deserializa al tipo que él mismo configura.
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, TransferMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSUMER
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, TransferMessage> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, bankCode + "-consumer-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Ignorar el header __TypeId__ del mensaje (no existe porque el producer
        // lo desactivó). Deserializar directamente al tipo TransferMessage local.
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransferMessage.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // ErrorHandlingDeserializer envuelve al JsonDeserializer para que un mensaje
        // malformado no bloquee toda la partición — lo registra y sigue adelante.
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferMessage> transferKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransferMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // ACK manual: el offset se confirma solo cuando el mensaje se procesó exitosamente
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // Concurrencia: 3 hilos procesando particiones en paralelo
        factory.setConcurrency(3);
        return factory;
    }
}
