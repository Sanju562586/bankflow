package com.bankflow.account.repository;

import com.bankflow.account.model.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountIdOrderByTimestampDesc(String accountId);

    Page<AccountTransaction> findByAccountIdOrderByTimestampDesc(String accountId, Pageable pageable);

    List<AccountTransaction> findByTransactionId(String transactionId);
}
