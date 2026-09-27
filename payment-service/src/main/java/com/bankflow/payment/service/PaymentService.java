package com.bankflow.payment.service;

import com.bankflow.payment.dto.PaymentRequest;
import com.bankflow.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request, String idempotencyKey, String clientIp, String userAgent);
    PaymentResponse getPaymentById(String transactionId);
    PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey);
    List<PaymentResponse> getAllPayments();
    List<PaymentResponse> getPaymentsForAccount(String accountId);
}
