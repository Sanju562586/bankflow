package com.bankflow.payment.controller;

import com.bankflow.payment.dto.PaymentRequest;
import com.bankflow.payment.dto.PaymentResponse;
import com.bankflow.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Submit a payment transfer request.
     * Enforces Idempotency-Key header to prevent duplicate debits.
     * Publishes TransactionInitiated to Kafka topic 'txn-events'.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = servletRequest.getHeader("X-Idempotency-Key");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // Auto-generate key if client omitted it for convenience
            idempotencyKey = "auto-idemp-" + System.currentTimeMillis() + "-" + request.getFromAccountId();
        }

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get payment status and saga details by transaction ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable("id") String id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Lookup payment by Idempotency Key.
     */
    @GetMapping("/idempotency/{key}")
    public ResponseEntity<PaymentResponse> getByIdempotency(@PathVariable("key") String key) {
        PaymentResponse response = paymentService.getPaymentByIdempotencyKey(key);
        return ResponseEntity.ok(response);
    }

    /**
     * List all payments.
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * List payments for a given account.
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByAccount(@PathVariable("accountId") String accountId) {
        return ResponseEntity.ok(paymentService.getPaymentsForAccount(accountId));
    }
}
