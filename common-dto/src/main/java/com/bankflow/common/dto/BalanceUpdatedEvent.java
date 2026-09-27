package com.bankflow.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class BalanceUpdatedEvent implements Serializable {
    private String accountId;
    private String transactionId;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private BigDecimal deltaAmount;
    private String operationType; // CREDIT or DEBIT
    private Instant timestamp;

    public BalanceUpdatedEvent() {}

    public BalanceUpdatedEvent(String accountId, String transactionId, BigDecimal previousBalance, 
                               BigDecimal newBalance, BigDecimal deltaAmount, String operationType, Instant timestamp) {
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.deltaAmount = deltaAmount;
        this.operationType = operationType;
        this.timestamp = timestamp;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public BigDecimal getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(BigDecimal previousBalance) { this.previousBalance = previousBalance; }

    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }

    public BigDecimal getDeltaAmount() { return deltaAmount; }
    public void setDeltaAmount(BigDecimal deltaAmount) { this.deltaAmount = deltaAmount; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
