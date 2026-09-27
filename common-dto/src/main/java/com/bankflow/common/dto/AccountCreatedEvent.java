package com.bankflow.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class AccountCreatedEvent implements Serializable {
    private String accountId;
    private String customerId;
    private String customerName;
    private String email;
    private String phoneNumber;
    private String currency;
    private BigDecimal initialBalance;
    private Instant createdAt;

    public AccountCreatedEvent() {}

    public AccountCreatedEvent(String accountId, String customerId, String customerName, String email, 
                               String phoneNumber, String currency, BigDecimal initialBalance, Instant createdAt) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.currency = currency;
        this.initialBalance = initialBalance;
        this.createdAt = createdAt;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
