package com.bankflow.payment.repository;

import com.bankflow.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<PaymentTransaction> findByFromAccountIdOrderByCreatedAtDesc(String fromAccountId);

    List<PaymentTransaction> findByToAccountIdOrderByCreatedAtDesc(String toAccountId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM PaymentTransaction p WHERE p.fromAccountId = :accountId OR p.toAccountId = :accountId ORDER BY p.createdAt DESC")
    List<PaymentTransaction> findByAccountIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("accountId") String accountId);
}
