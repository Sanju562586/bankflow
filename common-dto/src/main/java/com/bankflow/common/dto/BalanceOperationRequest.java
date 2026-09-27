package com.bankflow.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class BalanceOperationRequest implements Serializable {

    private String transactionId;
    private BigDecimal amount;
    private String operationType; // CREDIT or DEBIT
    private String description;
    private String counterpartyAccountId;

    public BalanceOperationRequest() {}

    public BalanceOperationRequest(String transactionId, BigDecimal amount, String operationType, 
                                   String description, String counterpartyAccountId) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.operationType = operationType;
        this.description = description;
        this.counterpartyAccountId = counterpartyAccountId;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCounterpartyAccountId() { return counterpartyAccountId; }
    public void setCounterpartyAccountId(String counterpartyAccountId) { this.counterpartyAccountId = counterpartyAccountId; }
}
