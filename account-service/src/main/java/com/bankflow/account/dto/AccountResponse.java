package com.bankflow.account.dto;

import com.bankflow.account.model.Account;
import java.math.BigDecimal;
import java.time.Instant;

public class AccountResponse {
    private String id;
    private String accountNumber;
    private String customerId;
    private String customerName;
    private String email;
    private String phoneNumber;
    private String currency;
    private BigDecimal balance;
    private String status;
    private Instant createdAt;

    public AccountResponse() {}

    public static AccountResponse fromEntity(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setCustomerId(account.getCustomerId());
        response.setCustomerName(account.getCustomerName());
        response.setEmail(account.getEmail());
        response.setPhoneNumber(account.getPhoneNumber());
        response.setCurrency(account.getCurrency());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

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

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
