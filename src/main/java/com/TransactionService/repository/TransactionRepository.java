package com.TransactionService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.TransactionService.model.Transaction;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionReference(String transactionReference);
    Optional<Transaction> findByFromAccount(String fromAccount);
    Optional<Transaction> findByToAccount(String toAccount);
    Optional<Transaction> findByTransactionTime(LocalDateTime transactionTime);
}
