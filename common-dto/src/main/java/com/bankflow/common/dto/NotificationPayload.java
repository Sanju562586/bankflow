package com.bankflow.common.dto;

import com.bankflow.common.enums.NotificationChannel;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public class NotificationPayload implements Serializable {
    private String notificationId;
    private String recipient;
    private String customerId;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private String priority;
    private Map<String, String> metadata;
    private Instant createdAt;

    public NotificationPayload() {}

    public NotificationPayload(String notificationId, String recipient, String customerId, 
                               NotificationChannel channel, String subject, String body, 
                               String priority, Map<String, String> metadata, Instant createdAt) {
        this.notificationId = notificationId;
        this.recipient = recipient;
        this.customerId = customerId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

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

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
