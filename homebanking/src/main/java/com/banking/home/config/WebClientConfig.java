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

    /** Token de servicio con el que el Home Banking se identifica ante el Core. */
    @Value("${banking.service-token:}")
    private String serviceToken;

    /**
     * Cliente para comunicarse con el Core Banking de este banco.
     * La URL apunta al gateway que protege la instancia del banco; el Core
     * exige la cabecera X-Service-Token para aceptar las llamadas /api/**.
     */
    @Bean
    public WebClient coreBankingClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(coreBankUrl)
                .defaultHeader("Content-Type", "application/json");

        // Identifica al Home Banking ante el Core mediante el token de servicio
        if (serviceToken != null && !serviceToken.isBlank()) {
            builder.defaultHeader("X-Service-Token", serviceToken);
        }

        return builder.build();
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
