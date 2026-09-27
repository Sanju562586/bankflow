package com.bankflow.notification.model;

import com.bankflow.common.enums.NotificationChannel;
import java.time.Instant;

public class NotificationRecord {

    private String id;
    private String recipient;
    private String customerId;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private String status; // DELIVERED, RETRYING, FAILED
    private int attemptCount;
    private String failureReason;
    private Instant createdAt = Instant.now();
    private Instant deliveredAt;

    public NotificationRecord() {}

    public NotificationRecord(String id, String recipient, String customerId, 
                              NotificationChannel channel, String subject, String body) {
        this.id = id;
        this.recipient = recipient;
        this.customerId = customerId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = "PENDING";
        this.attemptCount = 0;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
