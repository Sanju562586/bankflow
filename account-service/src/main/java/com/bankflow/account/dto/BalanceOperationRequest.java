package com.bankflow.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BalanceOperationRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be strictly positive")
    private BigDecimal amount;

    @NotBlank(message = "Operation type is required (CREDIT or DEBIT)")
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
