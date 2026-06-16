package com.banking.core.repository;

import com.banking.core.domain.Transaction;
import com.banking.core.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    /** Historial de transacciones de una cuenta específica */
    List<Transaction> findBySourceAccountAccountNumberOrderByCreatedAtDesc(String accountNumber);

    /** Para encontrar transacciones pendientes y reintentar en caso de fallo */
    List<Transaction> findByStatus(TransactionStatus status);
}
