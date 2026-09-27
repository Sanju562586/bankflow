package com.bankflow.payment.event;

import com.bankflow.common.dto.TransactionInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${bankflow.kafka.topics.txn-events:txn-events}")
    private String txnEventsTopic;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTransactionInitiated(TransactionInitiatedEvent event) {
        log.info("Publishing TransactionInitiated event for txnId={}, idempotencyKey={}, amount={}",
                 event.getTransactionId(), event.getIdempotencyKey(), event.getAmount());
        try {
            kafkaTemplate.send(txnEventsTopic, event.getTransactionId(), event);
        } catch (Exception e) {
            log.error("Failed to publish TransactionInitiated to Kafka topic {}: {}", txnEventsTopic, e.getMessage(), e);
        }
    }
}
