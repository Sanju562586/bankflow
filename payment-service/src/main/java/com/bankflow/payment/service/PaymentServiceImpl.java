package com.bankflow.payment.service;

import com.bankflow.common.dto.TransactionInitiatedEvent;
import com.bankflow.common.enums.PaymentMode;
import com.bankflow.common.enums.TransactionStatus;
import com.bankflow.payment.dto.PaymentRequest;
import com.bankflow.payment.dto.PaymentResponse;
import com.bankflow.payment.event.PaymentEventProducer;
import com.bankflow.payment.exception.DuplicateRequestException;
import com.bankflow.payment.exception.PaymentNotFoundException;
import com.bankflow.payment.model.IdempotencyRecord;
import com.bankflow.payment.model.PaymentTransaction;
import com.bankflow.payment.repository.IdempotencyRecordRepository;
import com.bankflow.payment.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentTransactionRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    // Mode-specific limits
    private static final BigDecimal UPI_LIMIT = new BigDecimal("100000.00");
    private static final BigDecimal IMPS_LIMIT = new BigDecimal("500000.00");

    public PaymentServiceImpl(PaymentTransactionRepository paymentRepository,
                              IdempotencyRecordRepository idempotencyRepository,
                              PaymentEventProducer eventProducer,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey, String clientIp, String userAgent) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is mandatory for payment initiation");
        }

        // 1. Check idempotency record
        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            log.warn("Duplicate request detected for idempotency key {}", idempotencyKey);
            IdempotencyRecord record = existingRecord.get();
            if ("PROCESSED".equals(record.getStatus()) && record.getCachedResponse() != null) {
                try {
                    return objectMapper.readValue(record.getCachedResponse(), PaymentResponse.class);
                } catch (Exception e) {
                    log.error("Failed to deserialize cached response for key {}: {}", idempotencyKey, e.getMessage());
                }
            }
            throw new DuplicateRequestException("Duplicate transaction request with Idempotency-Key: " + idempotencyKey);
        }

        // 2. Validate transfer limits by mode
        validatePaymentLimits(request);

        // 3. Prevent self-transfers
        if (request.getFromAccountId().equalsIgnoreCase(request.getToAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be identical");
        }

        // 4. Reserve Idempotency Key
        String transactionId = "txn-" + UUID.randomUUID().toString();
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(idempotencyKey, transactionId, "IN_PROGRESS");
        try {
            idempotencyRepository.saveAndFlush(idempotencyRecord);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateRequestException("Concurrent request with Idempotency-Key: " + idempotencyKey);
        }

        // 5. Create PaymentTransaction
        PaymentTransaction transaction = new PaymentTransaction(
            transactionId,
            idempotencyKey,
            request.getFromAccountId(),
            request.getToAccountId(),
            request.getAmount(),
            request.getCurrency(),
            request.getMode(),
            request.getRemarks()
        );

        PaymentTransaction saved = paymentRepository.save(transaction);
        log.info("Initiated payment: id={}, idempotency={}, mode={}, amount={}", 
                 saved.getId(), idempotencyKey, saved.getMode(), saved.getAmount());

        PaymentResponse response = PaymentResponse.fromEntity(saved);

        // Cache response in idempotency record
        try {
            idempotencyRecord.setStatus("PROCESSED");
            idempotencyRecord.setCachedResponse(objectMapper.writeValueAsString(response));
            idempotencyRepository.save(idempotencyRecord);
        } catch (Exception e) {
            log.warn("Could not cache idempotency response: {}", e.getMessage());
        }

        // 6. Publish TransactionInitiatedEvent to Kafka
        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
            saved.getId(),
            saved.getIdempotencyKey(),
            saved.getFromAccountId(),
            saved.getToAccountId(),
            saved.getAmount(),
            saved.getCurrency(),
            saved.getMode(),
            clientIp != null ? clientIp : "127.0.0.1",
            UUID.randomUUID().toString().substring(0, 16),
            userAgent != null ? userAgent : "Bankflow-Client/1.0",
            saved.getCreatedAt()
        );
        eventProducer.publishTransactionInitiated(event);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String transactionId) {
        PaymentTransaction txn = paymentRepository.findById(transactionId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found: " + transactionId));
        return PaymentResponse.fromEntity(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        PaymentTransaction txn = paymentRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new PaymentNotFoundException("Payment transaction not found with idempotency key: " + idempotencyKey));
        return PaymentResponse.fromEntity(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
            .map(PaymentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForAccount(String accountId) {
        List<PaymentTransaction> list = paymentRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId);
        list.addAll(paymentRepository.findByToAccountIdOrderByCreatedAtDesc(accountId));
        return list.stream()
            .distinct()
            .map(PaymentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    private void validatePaymentLimits(PaymentRequest request) {
        if (request.getMode() == PaymentMode.UPI && request.getAmount().compareTo(UPI_LIMIT) > 0) {
            throw new IllegalArgumentException("UPI transfer exceeds maximum allowable limit of ₹" + UPI_LIMIT);
        }
        if (request.getMode() == PaymentMode.IMPS && request.getAmount().compareTo(IMPS_LIMIT) > 0) {
            throw new IllegalArgumentException("IMPS transfer exceeds maximum allowable limit of ₹" + IMPS_LIMIT);
        }
    }
}
