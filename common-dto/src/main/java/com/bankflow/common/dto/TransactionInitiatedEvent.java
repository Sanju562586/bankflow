package com.bankflow.common.dto;

import com.bankflow.common.enums.PaymentMode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionInitiatedEvent implements Serializable {
    private String transactionId;
    private String idempotencyKey;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentMode paymentMode;
    private String ipAddress;
    private String deviceFingerprint;
    private String userAgent;
    private Instant initiatedAt;

    public TransactionInitiatedEvent() {}

    public TransactionInitiatedEvent(String transactionId, String idempotencyKey, String fromAccountId, 
                                     String toAccountId, BigDecimal amount, String currency, PaymentMode paymentMode, 
                                     String ipAddress, String deviceFingerprint, String userAgent, Instant initiatedAt) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMode = paymentMode;
        this.ipAddress = ipAddress;
        this.deviceFingerprint = deviceFingerprint;
        this.userAgent = userAgent;
        this.initiatedAt = initiatedAt;
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

    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
}
