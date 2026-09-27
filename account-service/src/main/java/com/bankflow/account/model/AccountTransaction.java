package com.bankflow.account.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_transactions", indexes = {
    @Index(name = "idx_txns_account_id", columnList = "accountId"),
    @Index(name = "idx_txns_timestamp", columnList = "timestamp")
})
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 64)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String type; // CREDIT or DEBIT

    @Column(length = 256)
    private String description;

    @Column(length = 64)
    private String counterpartyAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public AccountTransaction() {}

    public AccountTransaction(String transactionId, String accountId, BigDecimal amount, 
                              String type, String description, String counterpartyAccountId, 
                              BigDecimal balanceAfter) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.counterpartyAccountId = counterpartyAccountId;
        this.balanceAfter = balanceAfter;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCounterpartyAccountId() { return counterpartyAccountId; }
    public void setCounterpartyAccountId(String counterpartyAccountId) { this.counterpartyAccountId = counterpartyAccountId; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
