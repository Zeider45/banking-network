package com.banking.switch_;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Banking Switch: el corazón de la red interbancaria.
 *
 * Este servicio actúa como intermediario entre los bancos:
 *  1. Escucha el topic "switch.transfers" donde los bancos publican transferencias salientes
 *  2. Determina el banco destino según el campo targetBankCode del mensaje
 *  3. Enruta el mensaje al topic "bank.{targetBankCode}.incoming" del banco destino
 *
 * Es stateless: no tiene base de datos propia, solo enruta mensajes.
 * El registro de bancos se gestiona en memoria (en producción usaría un registro externo).
 */
@SpringBootApplication
public class BankingSwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSwitchApplication.class, args);
    }
}
