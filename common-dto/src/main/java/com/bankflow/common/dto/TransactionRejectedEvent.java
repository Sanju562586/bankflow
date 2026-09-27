package com.bankflow.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionRejectedEvent implements Serializable {
    private String transactionId;
    private String idempotencyKey;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String rejectionReason;
    private double riskScore;
    private Instant rejectedAt;

    public TransactionRejectedEvent() {}

    public TransactionRejectedEvent(String transactionId, String idempotencyKey, String fromAccountId, 
                                    String toAccountId, BigDecimal amount, String rejectionReason, 
                                    double riskScore, Instant rejectedAt) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.rejectionReason = rejectionReason;
        this.riskScore = riskScore;
        this.rejectedAt = rejectedAt;
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

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
}
