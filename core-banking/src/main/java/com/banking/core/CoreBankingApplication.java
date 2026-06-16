package com.banking.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada del servicio Core Banking.
 *
 * Este servicio representa el núcleo de un banco: gestiona cuentas,
 * procesa depósitos, retiros y transferencias, y se comunica con otros
 * bancos a través de Kafka mediante el Banking Switch.
 *
 * Puede desplegarse múltiples veces (bank-a, bank-b…) cambiando solo
 * las variables de entorno: BANK_CODE, BANK_NAME y la base de datos.
 */
@SpringBootApplication
@EnableAsync  // Permite procesar eventos Kafka en hilos separados
public class CoreBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApplication.class, args);
    }
}
