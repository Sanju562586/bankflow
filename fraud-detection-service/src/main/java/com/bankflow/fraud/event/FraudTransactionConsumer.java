package com.bankflow.fraud.event;

import com.bankflow.common.dto.TransactionInitiatedEvent;
import com.bankflow.fraud.service.FraudDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FraudTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudTransactionConsumer.class);

    private final FraudDetectionService fraudDetectionService;

    public FraudTransactionConsumer(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @KafkaListener(topics = "${bankflow.kafka.topics.txn-events:txn-events}", groupId = "fraud-detection-group")
    public void consumeTransactionInitiated(TransactionInitiatedEvent event) {
        log.info("Fraud consumer received TransactionInitiated event: txnId={}, amount={}, from={}, to={}", 
                 event.getTransactionId(), event.getAmount(), event.getFromAccountId(), event.getToAccountId());
        try {
            fraudDetectionService.evaluate(event);
        } catch (Exception ex) {
            log.error("Error evaluating fraud for transaction {}: {}", event.getTransactionId(), ex.getMessage(), ex);
        }
    }
}
