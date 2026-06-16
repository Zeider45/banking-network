# Banking Network

Red bancaria completa con Core Banking replicable, Banking Switch interbancario y Home Banking, orquestada con Docker.

## Arquitectura

```
                    ┌─────────────────────────────────────────┐
                    │           USUARIO FINAL                  │
                    └─────────────────────────────────────────┘
                                     │ REST + JWT
                    ┌────────────────▼────────────────────────┐
                    │         HOME BANKING :8100               │
                    │   - Login con JWT                        │
                    │   - Consultar saldo                      │
                    │   - Depositar / Retirar                  │
                    │   - Transferir (interno + interbancario) │
                    └──────────────┬──────────────────────────┘
                                   │ REST interno
          ┌────────────────────────▼──────────────────────────┐
          │              BANKING SWITCH :8090                   │
          │   - Registro de bancos (BANKA, BANKB, BANKC)       │
          │   - Enrutamiento via Kafka                         │
          └────────┬───────────────┬───────────────┬──────────┘
        Kafka      │             Kafka              │ Kafka
  ┌────────────────▼───┐   ┌─────▼──────────────┐  ┌▼──────────────────┐
  │  BANCO A (interno)  │   │  BANCO B (interno)  │  │  BANCO C (interno)│
  │  core-banking       │   │  core-banking       │  │  core-banking     │
  │  BANKA-ACC-000001   │   │  BANKB-ACC-000001   │  │  BANKC-ACC-000001 │
  │  ── PostgreSQL ──   │   │  ── MariaDB ────    │  │  ── MariaDB ───   │
  │  postgres-bank-a    │   │  mariadb-bank-b     │  │  mariadb-bank-c   │
  └─────────────────────┘   └─────────────────────┘  └───────────────────┘
  Core sin puerto público (red interna core-net). Acceso solo vía core-gateway
  y con cabecera X-Service-Token. El Home Banking apunta al gateway, no al Core.

El mismo jar core-banking.jar corre en PostgreSQL y MariaDB:
Hibernate 6 auto-detecta el driver y el dialecto desde la JDBC URL.

Monitoreo:
    Prometheus :9090 → scrapeea métricas de todos los servicios
    Loki       :3100 → centraliza logs (via Loki4j appender)
    Grafana    :3000 → dashboards de métricas y logs
    Kafka UI   :8180 → inspección de topics y mensajes
```

## Inicio rápido

```bash
# Levantar toda la red (primera vez: tarda ~5 min compilando)
docker compose up --build

# Siguiente vez (sin recompilar)
docker compose up

# Ver logs de un servicio específico
docker compose logs -f bank-a
docker compose logs -f banking-switch

# Detener y eliminar todo (incluyendo datos)
docker compose down -v
```

## Servicios y puertos

| Servicio          | Puerto | DB         | Descripción                             |
|-------------------|--------|------------|-----------------------------------------|
| homebanking-a     | 8100   | —          | Portal Banco Alpha (usuarios de BANKA)  |
| homebanking-b     | 8101   | —          | Portal Banco Beta  (usuarios de BANKB)  |
| homebanking-c     | 8102   | —          | Portal Banco Gamma (usuarios de BANKC)  |
| bank-a (BANKA)    | interno| PostgreSQL | Core Banking — sin puerto público (red core-net) |
| bank-b (BANKB)    | interno| MariaDB    | Core Banking — sin puerto público (red core-net) |
| bank-c (BANKC)    | interno| MariaDB    | Core Banking — sin puerto público (red core-net) |
| core-gateway      | interno| —          | Reverse proxy: único acceso al Core (:8080/8081/8082) |
| banking-switch    | 8090   | —          | Enrutador interbancario                 |
| Kafka UI          | 8180   | —          | Interfaz web de Kafka                   |
| Grafana           | 3000   | —          | Dashboards (admin/banking123)           |
| Prometheus        | 9090   | —          | Métricas                                |
| Loki              | 3100   | —          | Logs centralizados                      |

> **Nota de seguridad:** los Core Banking ya no publican puerto al host. Viven en
> la red interna `core-net` y solo el `core-gateway` puede alcanzarlos. Además, el
> Home Banking debe identificarse ante el Core enviando la cabecera
> `X-Service-Token` (token de servicio / API key). Ver la sección
> *Seguridad servicio-a-servicio* más abajo.

## Datos de prueba (se crean automáticamente)

**Banco A — PostgreSQL (BANKA)**
- Usuario: `juan@banka.com` / `password123` → Cuenta: `BANKA-ACC-000001` (saldo: $10,000)
- Usuario: `maria@banka.com` / `password123` → Cuenta: `BANKA-ACC-000002` (saldo: $5,000)

**Banco B — MariaDB (BANKB)**
- Usuario: `juan@bankb.com` / `password123` → Cuenta: `BANKB-ACC-000001` (saldo: $10,000)
- Usuario: `maria@bankb.com` / `password123` → Cuenta: `BANKB-ACC-000002` (saldo: $5,000)

**Banco C — MariaDB (BANKC)**
- Usuario: `juan@bankc.com` / `password123` → Cuenta: `BANKC-ACC-000001` (saldo: $10,000)
- Usuario: `maria@bankc.com` / `password123` → Cuenta: `BANKC-ACC-000002` (saldo: $5,000)

## Ejemplos de uso con curl

Cada banco tiene **su propio portal**. Los usuarios de BANKA entran a `:8100`,
los de BANKB a `:8101`, los de BANKC a `:8102`.

### 1. Login (portal de Banco A)
```bash
curl -X POST http://localhost:8100/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@banka.com","password":"password123","bankCode":"BANKA"}'
# → { "token": "eyJ...", "accountNumber": "BANKA-ACC-000001", ... }
TOKEN=<token_recibido>
```

### 2. Consultar saldo
```bash
curl http://localhost:8100/api/banking/account \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Depósito
```bash
curl -X POST http://localhost:8100/api/banking/deposit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500.00, "description": "Depósito en efectivo"}'
```

### 4. Retiro
```bash
curl -X POST http://localhost:8100/api/banking/withdrawal \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 200.00, "description": "Retiro cajero"}'
```

### 5. Transferencia interna (BANKA → BANKA, mismo banco)
```bash
curl -X POST http://localhost:8100/api/banking/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetAccountNumber": "BANKA-ACC-000002",
    "targetBankCode": "BANKA",
    "amount": 100.00,
    "description": "Transferencia a María"
  }'
```

### 6. Transferencia interbancaria (BANKA → BANKB vía Switch + Kafka)
```bash
# Juan (BANKA) transfiere a la cuenta de María en BANKB
curl -X POST http://localhost:8100/api/banking/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetAccountNumber": "BANKB-ACC-000002",
    "targetBankCode": "BANKB",
    "amount": 250.00,
    "description": "Pago interbancario"
  }'
# María puede verificar en su portal (BANKB → puerto 8101)
```

### 7. Login en Banco B y verificar saldo recibido
```bash
curl -X POST http://localhost:8101/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@bankb.com","password":"password123","bankCode":"BANKB"}'
TOKEN_B=<token_recibido>

curl http://localhost:8101/api/banking/account \
  -H "Authorization: Bearer $TOKEN_B"
```

### 8. Ver bancos disponibles (desde cualquier portal)
```bash
curl http://localhost:8100/api/banking/banks -H "Authorization: Bearer $TOKEN"
```

### 9. Historial de movimientos
```bash
curl http://localhost:8100/api/banking/account/history -H "Authorization: Bearer $TOKEN"
```

## Monitoreo

- **Grafana**: http://localhost:3000 (admin / banking123)
  - Dashboard "Banking Network Overview" con métricas de transacciones y logs de error
- **Kafka UI**: http://localhost:8180
  - Ver topics: `switch.transfers`, `bank.BANKA.incoming`, `bank.BANKB.incoming`
- **Prometheus**: http://localhost:9090
  - Métrica clave: `banking_transactions_total{type,bank}`

## Flujo de una transferencia interbancaria

```
Usuario → HomeB (:8100)
  └─► POST /api/banking/transfer { targetBankCode: "BANKB" }
        └─► core-gateway  (+ cabecera X-Service-Token)
              └─► CoreBanking-A (interno, core-net)
                    └─► Debita BANKA-ACC-000001
                    └─► Publica en Kafka topic: switch.transfers
                          └─► BankingSwitch (:8090)
                                └─► Consume switch.transfers
                                └─► Detecta targetBankCode=BANKB
                                └─► Publica en Kafka topic: bank.BANKB.incoming
                                      └─► CoreBanking-B (interno, core-net)
                                            └─► Acredita BANKB-ACC-000001
                                            └─► Registra transacción COMPLETED
```

## Seguridad servicio-a-servicio

El Core Banking es un servicio interno y se protege con tres capas
complementarias (defensa en profundidad):

1. **Sin puerto público.** Los Core (`bank-a/b/c`) no declaran `ports:` en
   `docker-compose.yml`, así que no son accesibles desde el host.

2. **Red interna aislada (`core-net`).** Los Core y sus bases de datos viven en
   una red `internal: true`. El Home Banking **no** está en esa red: el único
   componente que puede alcanzar al Core es el `core-gateway`. La infraestructura
   compartida (Kafka, Loki, Prometheus) está conectada a ambas redes para que el
   Core pueda producir/consumir mensajes, enviar logs y exponer métricas.

3. **Token de servicio (API key).** El Core exige la cabecera `X-Service-Token`
   en todos los endpoints `/api/**`. El Home Banking la inyecta automáticamente
   en cada llamada. Los endpoints de `/actuator/**` quedan exentos para que
   Prometheus y los healthchecks sigan funcionando.

   - Core: propiedad `banking.service-token` (variable `SERVICE_TOKEN`),
     validada por `ServiceTokenFilter`.
   - Home Banking: propiedad `banking.service-token` (variable `SERVICE_TOKEN`),
     enviada por `coreBankingClient`.
   - **Ambos valores deben coincidir.** Cámbialos definiendo `SERVICE_TOKEN`
     (por ejemplo en un archivo `.env`) antes de `docker compose up`.

```
Home Banking ──(X-Service-Token)──► core-gateway ──► Core Banking (/api/**)
                                                       Actuator (/actuator/**) sin token
```

## Agregar un tercer banco

1. Duplicar el servicio `bank-b` en `docker-compose.yml` con `BANK_CODE: BANKC`
2. Agregar `BANKC` a la variable `REGISTERED_BANKS` del banking-switch
3. Agregar la nueva base de datos postgres
4. `docker compose up --build bank-c banking-switch`
