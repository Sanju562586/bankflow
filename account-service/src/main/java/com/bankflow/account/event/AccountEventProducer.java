package com.bankflow.account.event;

import com.bankflow.common.dto.AccountCreatedEvent;
import com.bankflow.common.dto.BalanceUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${bankflow.kafka.topics.account-events:account-events}")
    private String accountEventsTopic;

    @Value("${bankflow.kafka.topics.balance-updates:balance-updates}")
    private String balanceUpdatesTopic;

    public AccountEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAccountCreated(AccountCreatedEvent event) {
        log.info("Publishing AccountCreated event for accountId={}, customerId={}", 
                 event.getAccountId(), event.getCustomerId());
        try {
            kafkaTemplate.send(accountEventsTopic, event.getAccountId(), event);
        } catch (Exception e) {
            log.error("Failed to publish AccountCreated event to Kafka topic {}: {}", accountEventsTopic, e.getMessage(), e);
        }
    }

    public void publishBalanceUpdated(BalanceUpdatedEvent event) {
        log.info("Publishing BalanceUpdated event for accountId={}, txnId={}, newBalance={}", 
                 event.getAccountId(), event.getTransactionId(), event.getNewBalance());
        try {
            kafkaTemplate.send(balanceUpdatesTopic, event.getAccountId(), event);
        } catch (Exception e) {
            log.error("Failed to publish BalanceUpdated event to Kafka topic {}: {}", balanceUpdatesTopic, e.getMessage(), e);
        }
    }
}
