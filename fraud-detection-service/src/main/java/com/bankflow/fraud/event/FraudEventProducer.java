package com.bankflow.fraud.event;

import com.bankflow.common.dto.FraudAlertEvent;
import com.bankflow.common.dto.TransactionApprovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudEventProducer {

    private static final Logger log = LoggerFactory.getLogger(FraudEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${bankflow.kafka.topics.fraud-alerts:fraud-alerts}")
    private String fraudAlertsTopic;

    @Value("${bankflow.kafka.topics.txn-approved:txn-approved}")
    private String txnApprovedTopic;

    public FraudEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishFraudAlert(FraudAlertEvent event) {
        log.warn("Publishing FraudAlert event to topic {}: txnId={}, score={}, rules={}", 
                 fraudAlertsTopic, event.getTransactionId(), event.getRiskScore(), event.getTriggeredRules());
        try {
            kafkaTemplate.send(fraudAlertsTopic, event.getTransactionId(), event);
        } catch (Exception e) {
            log.error("Failed to publish FraudAlert to Kafka topic {}: {}", fraudAlertsTopic, e.getMessage(), e);
        }
    }

    public void publishTransactionApproved(TransactionApprovedEvent event) {
        log.info("Publishing TransactionApproved event to topic {}: txnId={}, score={}", 
                 txnApprovedTopic, event.getTransactionId(), event.getRiskScore());
        try {
            kafkaTemplate.send(txnApprovedTopic, event.getTransactionId(), event);
        } catch (Exception e) {
            log.error("Failed to publish TransactionApproved to Kafka topic {}: {}", txnApprovedTopic, e.getMessage(), e);
        }
    }
}
