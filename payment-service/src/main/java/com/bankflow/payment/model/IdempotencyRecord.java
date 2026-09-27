package com.bankflow.payment.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idemp_key", columnList = "idempotencyKey", unique = true)
})
public class IdempotencyRecord {

    @Id
    @Column(length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 32)
    private String status; // IN_PROGRESS, PROCESSED

    @Column(columnDefinition = "TEXT")
    private String cachedResponse;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String transactionId, String status) {
        this.idempotencyKey = idempotencyKey;
        this.transactionId = transactionId;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCachedResponse() { return cachedResponse; }
    public void setCachedResponse(String cachedResponse) { this.cachedResponse = cachedResponse; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
