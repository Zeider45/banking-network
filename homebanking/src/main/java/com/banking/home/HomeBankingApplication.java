package com.banking.home;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Home Banking: interfaz REST para usuarios finales.
 *
 * Expone endpoints protegidos con JWT para que los usuarios puedan:
 *  - Autenticarse (login)
 *  - Consultar sus cuentas y saldos
 *  - Realizar depósitos y retiros
 *  - Transferir dinero (mismo banco o inter-bancario vía Banking Switch)
 *
 * Este servicio NO tiene base de datos propia: actúa como API Gateway
 * que delega todas las operaciones al Core Banking correspondiente.
 */
@SpringBootApplication
public class HomeBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeBankingApplication.class, args);
    }
}
