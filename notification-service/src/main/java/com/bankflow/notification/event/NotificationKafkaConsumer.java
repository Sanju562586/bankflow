package com.bankflow.notification.event;

import com.bankflow.common.dto.FraudAlertEvent;
import com.bankflow.common.dto.TransactionApprovedEvent;
import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.model.NotificationRecord;
import com.bankflow.notification.service.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final NotificationDispatcher dispatcher;

    public NotificationKafkaConsumer(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Consumes high-priority FraudAlert events from Kafka.
     * Triggers multi-channel critical alert (SMS + Email + In-App Push).
     */
    @KafkaListener(topics = "${bankflow.kafka.topics.fraud-alerts:fraud-alerts}", groupId = "notification-fraud-group")
    public void handleFraudAlert(FraudAlertEvent event) {
        log.warn("Notification service received FraudAlert event for txnId={}, score={}", 
                 event.getTransactionId(), event.getRiskScore());

        String subject = "URGENT SECURITY ALERT: Suspicious Transaction Blocked";
        String body = String.format(
            "Bankflow Security: A transfer of ₹%s from account %s was flagged as high risk (Risk Score: %.2f) and blocked. %s",
            event.getAmount(), event.getFromAccountId(), event.getRiskScore(),
            event.getLlmExplanation() != null ? event.getLlmExplanation() : "Automated security rule triggered."
        );

        // 1. Send SMS Alert
        NotificationRecord sms = new NotificationRecord(
            "notif-sms-" + UUID.randomUUID().toString().substring(0, 8),
            "+919876543210", // In full system, retrieved from account-service
            event.getFromAccountId(),
            NotificationChannel.SMS,
            subject,
            body
        );
        try {
            dispatcher.dispatch(sms);
        } catch (Exception ignored) {}

        // 2. Send Urgent In-App Push
        NotificationRecord push = new NotificationRecord(
            "notif-push-" + UUID.randomUUID().toString().substring(0, 8),
            "device-user",
            event.getFromAccountId(),
            NotificationChannel.PUSH,
            "Security Alert: Blocked Transaction",
            "A suspicious attempt of ₹" + event.getAmount() + " was stopped."
        );
        try {
            dispatcher.dispatch(push);
        } catch (Exception ignored) {}

        // 3. Send Security Email
        NotificationRecord email = new NotificationRecord(
            "notif-email-" + UUID.randomUUID().toString().substring(0, 8),
            "customer@example.com",
            event.getFromAccountId(),
            NotificationChannel.EMAIL,
            subject,
            body + "\n\nIf you did not authorize this, please freeze your account immediately."
        );
        try {
            dispatcher.dispatch(email);
        } catch (Exception ignored) {}
    }

    /**
     * Consumes TransactionApproved events from Kafka.
     * Triggers confirmation Push and SMS receipt.
     */
    @KafkaListener(topics = "${bankflow.kafka.topics.txn-approved:txn-approved}", groupId = "notification-approved-group")
    public void handleTransactionApproved(TransactionApprovedEvent event) {
        log.info("Notification service received TransactionApproved event for txnId={}, amount={}", 
                 event.getTransactionId(), event.getAmount());

        String subject = "Transaction Successful";
        String body = String.format(
            "Bankflow Alert: ₹%s transferred successfully from %s to %s. Txn ID: %s.",
            event.getAmount(), event.getFromAccountId(), event.getToAccountId(), event.getTransactionId()
        );

        // 1. In-App Push
        NotificationRecord push = new NotificationRecord(
            "notif-push-" + UUID.randomUUID().toString().substring(0, 8),
            "device-user",
            event.getFromAccountId(),
            NotificationChannel.PUSH,
            subject,
            body
        );
        try {
            dispatcher.dispatch(push);
        } catch (Exception ignored) {}

        // 2. SMS Confirmation
        NotificationRecord sms = new NotificationRecord(
            "notif-sms-" + UUID.randomUUID().toString().substring(0, 8),
            "+919876543210",
            event.getFromAccountId(),
            NotificationChannel.SMS,
            subject,
            body
        );
        try {
            dispatcher.dispatch(sms);
        } catch (Exception ignored) {}
    }

    /**
     * Consumes TransactionRejected events from Kafka.
     * Triggers alert Push and SMS to inform customer of failure/rejection.
     */
    @KafkaListener(topics = "${bankflow.kafka.topics.txn-rejected:txn-rejected}", groupId = "notification-rejected-group")
    public void handleTransactionRejected(com.bankflow.common.dto.TransactionRejectedEvent event) {
        log.warn("Notification service received TransactionRejected event for txnId={}, reason={}", 
                 event.getTransactionId(), event.getRejectionReason());

        String subject = "Transaction Declined";
        String body = String.format(
            "Bankflow Alert: Transfer of ₹%s from account %s could not be processed. Reason: %s",
            event.getAmount(), event.getFromAccountId(),
            event.getRejectionReason() != null ? event.getRejectionReason() : "Declined by banking rules."
        );

        NotificationRecord push = new NotificationRecord(
            "notif-push-" + UUID.randomUUID().toString().substring(0, 8),
            "device-user",
            event.getFromAccountId(),
            NotificationChannel.PUSH,
            subject,
            body
        );
        try {
            dispatcher.dispatch(push);
        } catch (Exception ignored) {}

        NotificationRecord sms = new NotificationRecord(
            "notif-sms-" + UUID.randomUUID().toString().substring(0, 8),
            "+919876543210",
            event.getFromAccountId(),
            NotificationChannel.SMS,
            subject,
            body
        );
        try {
            dispatcher.dispatch(sms);
        } catch (Exception ignored) {}
    }
}
