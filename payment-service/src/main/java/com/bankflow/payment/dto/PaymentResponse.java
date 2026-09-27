package com.bankflow.payment.dto;

import com.bankflow.common.enums.PaymentMode;
import com.bankflow.common.enums.TransactionStatus;
import com.bankflow.payment.model.PaymentTransaction;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {
    private String transactionId;
    private String idempotencyKey;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentMode mode;
    private TransactionStatus status;
    private String sagaState;
    private Double riskScore;
    private String rejectionReason;
    private String remarks;
    private Instant createdAt;

    public PaymentResponse() {}

    public static PaymentResponse fromEntity(PaymentTransaction txn) {
        PaymentResponse resp = new PaymentResponse();
        resp.setTransactionId(txn.getId());
        resp.setIdempotencyKey(txn.getIdempotencyKey());
        resp.setFromAccountId(txn.getFromAccountId());
        resp.setToAccountId(txn.getToAccountId());
        resp.setAmount(txn.getAmount());
        resp.setCurrency(txn.getCurrency());
        resp.setMode(txn.getMode());
        resp.setStatus(txn.getStatus());
        resp.setSagaState(txn.getSagaState());
        resp.setRiskScore(txn.getRiskScore());
        resp.setRejectionReason(txn.getRejectionReason());
        resp.setRemarks(txn.getRemarks());
        resp.setCreatedAt(txn.getCreatedAt());
        return resp;
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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentMode getMode() { return mode; }
    public void setMode(PaymentMode mode) { this.mode = mode; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getSagaState() { return sagaState; }
    public void setSagaState(String sagaState) { this.sagaState = sagaState; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
