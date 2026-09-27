package com.bankflow.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionApprovedEvent implements Serializable {
    private String transactionId;
    private String idempotencyKey;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private double riskScore;
    private String approvalReason;
    private Instant approvedAt;

    public TransactionApprovedEvent() {}

    public TransactionApprovedEvent(String transactionId, String idempotencyKey, String fromAccountId, 
                                    String toAccountId, BigDecimal amount, double riskScore, 
                                    String approvalReason, Instant approvedAt) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.riskScore = riskScore;
        this.approvalReason = approvalReason;
        this.approvedAt = approvedAt;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }

    public String getToAccountId() { return toAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public String getApprovalReason() { return approvalReason; }
    public void setApprovalReason(String approvalReason) { this.approvalReason = approvalReason; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
}
