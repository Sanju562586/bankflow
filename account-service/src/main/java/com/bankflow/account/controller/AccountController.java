package com.bankflow.account.controller;

import com.bankflow.account.dto.AccountResponse;
import com.bankflow.account.dto.BalanceOperationRequest;
import com.bankflow.account.dto.CreateAccountRequest;
import com.bankflow.account.model.AccountTransaction;
import com.bankflow.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create a new bank account.
     * Publishes AccountCreated event to Kafka.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve account details by ID or Account Number.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable("id") String id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List all accounts (convenient for testing and web dashboard).
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Balance inquiry for account.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable("id") String id) {
        AccountResponse account = accountService.getAccountById(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("accountId", account.getId());
        resp.put("accountNumber", account.getAccountNumber());
        resp.put("currency", account.getCurrency());
        resp.put("balance", account.getBalance());
        return ResponseEntity.ok(resp);
    }

    /**
     * Credit / debit balance operation (e.g. from payment saga or direct adjustment).
     * Publishes BalanceUpdated event to Kafka.
     */
    @PostMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> updateBalance(
            @PathVariable("id") String id,
            @Valid @RequestBody BalanceOperationRequest request) {
        AccountResponse updated = accountService.updateBalance(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get transaction history for an account.
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<AccountTransaction>> getTransactions(
            @PathVariable("id") String id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<AccountTransaction> txns = accountService.getTransactionHistoryPaged(id, PageRequest.of(page, size));
        return ResponseEntity.ok(txns.getContent());
    }
}
