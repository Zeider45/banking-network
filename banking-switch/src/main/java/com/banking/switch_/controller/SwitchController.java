package com.banking.switch_.controller;

import com.banking.switch_.dto.BankInfo;
import com.banking.switch_.service.BankRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST del Banking Switch para consulta y administración del registro de bancos.
 * El Home Banking consulta este endpoint para listar los bancos disponibles.
 */
@RestController
@RequestMapping("/api/switch")
@RequiredArgsConstructor
public class SwitchController {

    private final BankRegistryService bankRegistry;

    /** Lista todos los bancos registrados en el Switch */
    @GetMapping("/banks")
    public ResponseEntity<List<BankInfo>> getBanks() {
        return ResponseEntity.ok(bankRegistry.getAllBanks());
    }

    /** Consulta información de un banco específico */
    @GetMapping("/banks/{bankCode}")
    public ResponseEntity<BankInfo> getBank(@PathVariable String bankCode) {
        return bankRegistry.findBank(bankCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Registra un nuevo banco (endpoint de administración) */
    @PostMapping("/banks")
    public ResponseEntity<Void> registerBank(@RequestBody BankInfo bank) {
        bankRegistry.registerBank(bank);
        return ResponseEntity.ok().build();
    }
}
