package com.banking.switch_.service;

import com.banking.switch_.dto.BankInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro central de bancos conocidos por el Switch.
 *
 * En este diseño el registro se inicializa desde variables de entorno
 * (lista separada por comas en REGISTERED_BANKS).
 *
 * En producción este servicio consultaría un service registry externo
 * (Consul, etcd, base de datos) para descubrimiento dinámico de bancos.
 */
@Service
@Slf4j
public class BankRegistryService {

    /**
     * Mapa de bankCode → BankInfo.
     * ConcurrentHashMap para acceso thread-safe desde el consumer de Kafka.
     */
    private final Map<String, BankInfo> registry = new ConcurrentHashMap<>();

    /**
     * Lista de bancos registrados inyectada desde la variable de entorno.
     * Formato: "BANKA,BANKB,BANKC"
     */
    public BankRegistryService(@Value("${banking.registered-banks:BANKA,BANKB}") String registeredBanks) {
        // Construye el registro a partir de la lista de códigos
        for (String code : registeredBanks.split(",")) {
            String bankCode = code.trim().toUpperCase();
            String incomingTopic = "bank." + bankCode + ".incoming";

            BankInfo info = new BankInfo(bankCode, "Banco " + bankCode, incomingTopic, true);
            registry.put(bankCode, info);

            log.info("Banco registrado: {} → topic={}", bankCode, incomingTopic);
        }
    }

    /** Busca un banco por su código */
    public Optional<BankInfo> findBank(String bankCode) {
        return Optional.ofNullable(registry.get(bankCode.toUpperCase()));
    }

    /** Lista todos los bancos registrados */
    public List<BankInfo> getAllBanks() {
        return List.copyOf(registry.values());
    }

    /** Registra un nuevo banco dinámicamente (ej: desde un endpoint de administración) */
    public void registerBank(BankInfo bank) {
        registry.put(bank.getBankCode().toUpperCase(), bank);
        log.info("Nuevo banco registrado dinámicamente: {}", bank.getBankCode());
    }

    /** Desactiva un banco (sus transferencias serán rechazadas) */
    public void deactivateBank(String bankCode) {
        BankInfo bank = registry.get(bankCode.toUpperCase());
        if (bank != null) {
            bank.setActive(false);
            log.warn("Banco desactivado: {}", bankCode);
        }
    }
}
