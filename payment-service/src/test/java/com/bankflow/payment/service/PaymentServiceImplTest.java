package com.bankflow.payment.service;

import com.bankflow.common.enums.PaymentMode;
import com.bankflow.payment.dto.PaymentRequest;
import com.bankflow.payment.dto.PaymentResponse;
import com.bankflow.payment.event.PaymentEventProducer;
import com.bankflow.payment.exception.DuplicateRequestException;
import com.bankflow.payment.model.IdempotencyRecord;
import com.bankflow.payment.model.PaymentTransaction;
import com.bankflow.payment.repository.IdempotencyRecordRepository;
import com.bankflow.payment.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRepository;

    @Mock
    private PaymentEventProducer eventProducer;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, idempotencyRepository, eventProducer, new ObjectMapper());
    }

    @Test
    @DisplayName("processPayment: successfully initiates transfer and publishes event")
    void testProcessPaymentSuccess() {
        PaymentRequest req = new PaymentRequest("acc-1", "acc-2", new BigDecimal("500.00"), PaymentMode.UPI, "Dinner");

        when(idempotencyRepository.findByIdempotencyKey("idemp-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.saveAndFlush(any(IdempotencyRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse resp = paymentService.processPayment(req, "idemp-1", "127.0.0.1", "TestClient/1.0");

        assertThat(resp).isNotNull();
        assertThat(resp.getFromAccountId()).isEqualTo("acc-1");
        assertThat(resp.getToAccountId()).isEqualTo("acc-2");
        assertThat(resp.getAmount()).isEqualByComparingTo("500.00");
        verify(eventProducer, times(1)).publishTransactionInitiated(any());
    }

    @Test
    @DisplayName("processPayment: throws IllegalArgumentException if Idempotency-Key is missing")
    void testProcessPaymentMissingIdempKey() {
        PaymentRequest req = new PaymentRequest("acc-1", "acc-2", new BigDecimal("500.00"), PaymentMode.UPI, "Dinner");

        assertThatThrownBy(() -> paymentService.processPayment(req, "", "127.0.0.1", "TestClient/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key header is mandatory");
    }

    @Test
    @DisplayName("processPayment: throws DuplicateRequestException if key already in progress")
    void testProcessPaymentDuplicateKey() {
        PaymentRequest req = new PaymentRequest("acc-1", "acc-2", new BigDecimal("500.00"), PaymentMode.UPI, "Dinner");
        IdempotencyRecord inProgressRecord = new IdempotencyRecord("idemp-1", "txn-1", "IN_PROGRESS");

        when(idempotencyRepository.findByIdempotencyKey("idemp-1")).thenReturn(Optional.of(inProgressRecord));

        assertThatThrownBy(() -> paymentService.processPayment(req, "idemp-1", "127.0.0.1", "TestClient/1.0"))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessageContaining("Duplicate transaction request");
    }

    @Test
    @DisplayName("processPayment: throws IllegalArgumentException on self-transfer")
    void testProcessPaymentSelfTransfer() {
        PaymentRequest req = new PaymentRequest("acc-1", "acc-1", new BigDecimal("500.00"), PaymentMode.UPI, "Self");

        assertThatThrownBy(() -> paymentService.processPayment(req, "idemp-1", "127.0.0.1", "TestClient/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be identical");
    }

    @Test
    @DisplayName("processPayment: throws IllegalArgumentException if UPI limit is exceeded")
    void testProcessPaymentUpiLimitExceeded() {
        PaymentRequest req = new PaymentRequest("acc-1", "acc-2", new BigDecimal("150000.00"), PaymentMode.UPI, "Too large");

        assertThatThrownBy(() -> paymentService.processPayment(req, "idemp-1", "127.0.0.1", "TestClient/1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UPI transfer exceeds maximum allowable limit");
    }

    @Test
    @DisplayName("getPaymentsForAccount: returns sorted list from repository")
    void testGetPaymentsForAccount() {
        PaymentTransaction tx = new PaymentTransaction("txn-1", "idemp-1", "acc-1", "acc-2", new BigDecimal("100.00"), "INR", PaymentMode.UPI, "Test");
        when(paymentRepository.findByAccountIdOrderByCreatedAtDesc("acc-1")).thenReturn(List.of(tx));

        List<PaymentResponse> list = paymentService.getPaymentsForAccount("acc-1");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTransactionId()).isEqualTo("txn-1");
    }
}
