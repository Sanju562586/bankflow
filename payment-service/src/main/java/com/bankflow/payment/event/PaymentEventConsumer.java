package com.bankflow.payment.event;

import com.bankflow.common.dto.FraudAlertEvent;
import com.bankflow.common.dto.TransactionApprovedEvent;
import com.bankflow.common.dto.TransactionRejectedEvent;
import com.bankflow.payment.saga.PaymentSagaCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentSagaCoordinator sagaCoordinator;

    public PaymentEventConsumer(PaymentSagaCoordinator sagaCoordinator) {
        this.sagaCoordinator = sagaCoordinator;
    }

    @KafkaListener(topics = "${bankflow.kafka.topics.fraud-alerts:fraud-alerts}", groupId = "payment-fraud-alert-group")
    public void handleFraudAlert(FraudAlertEvent event) {
        log.warn("Received FraudAlert event for txnId={}, riskScore={}, flaggedByLlm={}", 
                 event.getTransactionId(), event.getRiskScore(), event.isFlaggedByLlm());
        sagaCoordinator.executeRejectSaga(
            event.getTransactionId(), 
            "Fraud detected. Score: " + event.getRiskScore() + ". " + (event.getLlmExplanation() != null ? event.getLlmExplanation() : "Rule violation"),
            event.getRiskScore()
        );
    }

    @KafkaListener(topics = "${bankflow.kafka.topics.txn-approved:txn-approved}", groupId = "payment-txn-approved-group")
    public void handleTransactionApproved(TransactionApprovedEvent event) {
        log.info("Received TransactionApproved event for txnId={}, score={}", 
                 event.getTransactionId(), event.getRiskScore());
        sagaCoordinator.executeCommitSaga(event.getTransactionId(), event.getRiskScore());
    }

    @KafkaListener(topics = "${bankflow.kafka.topics.txn-rejected:txn-rejected}", groupId = "payment-txn-rejected-group")
    public void handleTransactionRejected(TransactionRejectedEvent event) {
        log.warn("Received TransactionRejected event for txnId={}, reason={}", 
                 event.getTransactionId(), event.getRejectionReason());
        sagaCoordinator.executeRejectSaga(event.getTransactionId(), event.getRejectionReason(), event.getRiskScore());
    }
}
