package com.banking.core.repository;

import com.banking.core.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Busca una cuenta por número.
     * Usamos PESSIMISTIC_WRITE para evitar race conditions en operaciones de saldo:
     * si dos transferencias llegan al mismo tiempo, solo una bloquea la fila primero.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(String accountNumber);

    /** Búsqueda sin lock (solo lectura, ej: consulta de saldo) */
    Optional<Account> findByAccountNumber(String accountNumber);

    /** Todas las cuentas de un usuario */
    List<Account> findByOwnerId(Long userId);

    boolean existsByAccountNumber(String accountNumber);
}
