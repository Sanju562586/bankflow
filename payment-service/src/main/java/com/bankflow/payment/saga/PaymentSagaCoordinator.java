package com.bankflow.payment.saga;

import com.bankflow.account.dto.BalanceOperationRequest;
import com.bankflow.common.enums.TransactionStatus;
import com.bankflow.payment.model.PaymentTransaction;
import com.bankflow.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class PaymentSagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaCoordinator.class);

    private final PaymentTransactionRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Value("${bankflow.services.account-url:http://localhost:8081}")
    private String accountServiceUrl;

    public PaymentSagaCoordinator(PaymentTransactionRepository paymentRepository, RestTemplateBuilder builder) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Executes Saga multi-step commit:
     * Step 1: Debit sender account
     * Step 2: Credit recipient account
     * Step 3: If Step 2 fails, compensate Step 1 (refund sender)
     */
    @Transactional
    public void executeCommitSaga(String transactionId, double riskScore) {
        PaymentTransaction txn = paymentRepository.findById(transactionId).orElse(null);
        if (txn == null) {
            log.error("Saga commit failed: transaction {} not found", transactionId);
            return;
        }

        if (txn.getStatus() == TransactionStatus.COMPLETED || txn.getStatus() == TransactionStatus.REJECTED) {
            log.info("Transaction {} is already in final state: {}", transactionId, txn.getStatus());
            return;
        }

        log.info("Starting Saga execution for transactionId={}, amount={}, from={}, to={}", 
                 txn.getId(), txn.getAmount(), txn.getFromAccountId(), txn.getToAccountId());

        txn.setRiskScore(riskScore);
        txn.setStatus(TransactionStatus.APPROVED);
        txn.setSagaState("DEBITING_SOURCE");
        paymentRepository.save(txn);

        boolean debited = false;
        try {
            // Step 1: Debit Source
            BalanceOperationRequest debitReq = new BalanceOperationRequest(
                txn.getId(),
                txn.getAmount(),
                "DEBIT",
                "Payment to " + txn.getToAccountId() + " via " + txn.getMode(),
                txn.getToAccountId()
            );
            String debitUrl = accountServiceUrl + "/accounts/" + txn.getFromAccountId() + "/balance";
            ResponseEntity<String> debitResp = restTemplate.postForEntity(debitUrl, debitReq, String.class);
            if (!debitResp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Debit failed with status " + debitResp.getStatusCode());
            }
            debited = true;
            txn.setSagaState("CREDITING_DESTINATION");
            paymentRepository.save(txn);

            // Step 2: Credit Destination
            BalanceOperationRequest creditReq = new BalanceOperationRequest(
                txn.getId(),
                txn.getAmount(),
                "CREDIT",
                "Received from " + txn.getFromAccountId() + " via " + txn.getMode(),
                txn.getFromAccountId()
            );
            String creditUrl = accountServiceUrl + "/accounts/" + txn.getToAccountId() + "/balance";
            ResponseEntity<String> creditResp = restTemplate.postForEntity(creditUrl, creditReq, String.class);
            if (!creditResp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Credit failed with status " + creditResp.getStatusCode());
            }

            // Step 3: Complete Saga
            txn.setStatus(TransactionStatus.COMPLETED);
            txn.setSagaState("COMPLETED");
            paymentRepository.save(txn);
            log.info("Saga successfully completed for transaction {}", transactionId);

        } catch (Exception ex) {
            log.error("Saga execution error for txn {}: {}", transactionId, ex.getMessage(), ex);

            if (debited) {
                // Compensating action: refund source account
                log.warn("Compensating Saga: refunding {} to account {}", txn.getAmount(), txn.getFromAccountId());
                try {
                    BalanceOperationRequest refundReq = new BalanceOperationRequest(
                        txn.getId() + "-refund",
                        txn.getAmount(),
                        "CREDIT",
                        "Refund for failed transfer " + txn.getId(),
                        txn.getToAccountId()
                    );
                    restTemplate.postForEntity(accountServiceUrl + "/accounts/" + txn.getFromAccountId() + "/balance", refundReq, String.class);
                    txn.setStatus(TransactionStatus.COMPENSATED);
                    txn.setSagaState("COMPENSATED");
                } catch (Exception refundEx) {
                    log.error("CRITICAL: Failed compensating refund for txn {}: {}", transactionId, refundEx.getMessage());
                    txn.setStatus(TransactionStatus.FAILED);
                    txn.setSagaState("COMPENSATION_FAILED");
                }
            } else {
                txn.setStatus(TransactionStatus.FAILED);
                txn.setSagaState("FAILED");
            }
            txn.setRejectionReason("Execution failure: " + ex.getMessage());
            paymentRepository.save(txn);
        }
    }

    @Transactional
    public void executeRejectSaga(String transactionId, String reason, double riskScore) {
        PaymentTransaction txn = paymentRepository.findById(transactionId).orElse(null);
        if (txn == null) return;

        log.warn("Rejecting payment transaction {}: reason={}, riskScore={}", transactionId, reason, riskScore);
        txn.setStatus(TransactionStatus.REJECTED);
        txn.setSagaState("FAILED");
        txn.setRiskScore(riskScore);
        txn.setRejectionReason(reason);
        paymentRepository.save(txn);
    }
}
