package com.banking.home.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configura los clientes HTTP para comunicarse con los servicios internos.
 *
 * WebClient (reactivo) se prefiere sobre RestTemplate para mejor manejo
 * de timeouts y operaciones asíncronas.
 */
@Configuration
public class WebClientConfig {

    @Value("${banking.core-bank-url}")
    private String coreBankUrl;

    @Value("${banking.switch-url}")
    private String switchUrl;

    /**
     * Cliente para comunicarse con el Core Banking de este banco.
     * La URL apunta a la instancia específica del banco (bank-a, bank-b, etc.)
     */
    @Bean
    public WebClient coreBankingClient() {
        return WebClient.builder()
                .baseUrl(coreBankUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Cliente para comunicarse con el Banking Switch.
     * Se usa para consultar el listado de bancos disponibles.
     */
    @Bean
    public WebClient switchClient() {
        return WebClient.builder()
                .baseUrl(switchUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
