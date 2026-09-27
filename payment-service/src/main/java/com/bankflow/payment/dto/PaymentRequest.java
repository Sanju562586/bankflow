package com.bankflow.payment.dto;

import com.bankflow.common.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotBlank(message = "Sender account ID is required")
    private String fromAccountId;

    @NotBlank(message = "Recipient account ID is required")
    private String toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be strictly positive")
    private BigDecimal amount;

    private String currency = "INR";

    @NotNull(message = "Payment mode is required (NEFT, UPI, IMPS)")
    private PaymentMode mode;

    private String remarks;

    public PaymentRequest() {}

    public PaymentRequest(String fromAccountId, String toAccountId, BigDecimal amount, PaymentMode mode, String remarks) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.mode = mode;
        this.remarks = remarks;
    }

    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }

    public String getToAccountId() { return toAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentMode getMode() { return mode; }
    public void setMode(PaymentMode mode) { this.mode = mode; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
