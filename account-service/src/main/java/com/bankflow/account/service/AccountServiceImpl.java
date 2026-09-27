package com.bankflow.account.service;

import com.bankflow.account.dto.AccountResponse;
import com.bankflow.common.dto.BalanceOperationRequest;
import com.bankflow.account.dto.CreateAccountRequest;
import com.bankflow.account.event.AccountEventProducer;
import com.bankflow.account.exception.AccountNotFoundException;
import com.bankflow.account.exception.InsufficientBalanceException;
import com.bankflow.account.model.Account;
import com.bankflow.account.model.AccountTransaction;
import com.bankflow.account.repository.AccountRepository;
import com.bankflow.account.repository.AccountTransactionRepository;
import com.bankflow.common.dto.AccountCreatedEvent;
import com.bankflow.common.dto.BalanceUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final SecureRandom random = new SecureRandom();

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final AccountEventProducer eventProducer;

    public AccountServiceImpl(AccountRepository accountRepository, 
                              AccountTransactionRepository transactionRepository, 
                              AccountEventProducer eventProducer) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventProducer = eventProducer;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountId = "acc-" + UUID.randomUUID().toString().substring(0, 8);
        String accountNumber = generateAccountNumber();

        Account account = new Account(
            accountId,
            accountNumber,
            request.getCustomerId(),
            request.getCustomerName(),
            request.getEmail(),
            request.getPhoneNumber(),
            request.getCurrency(),
            request.getInitialBalance()
        );

        Account saved = accountRepository.save(account);
        log.info("Created account with id={}, number={}, customer={}", saved.getId(), saved.getAccountNumber(), saved.getCustomerId());

        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            AccountTransaction initialTxn = new AccountTransaction(
                "txn-init-" + UUID.randomUUID().toString().substring(0, 8),
                saved.getId(),
                request.getInitialBalance(),
                "CREDIT",
                "Initial Deposit",
                "SYSTEM",
                saved.getBalance()
            );
            transactionRepository.save(initialTxn);
        }

        AccountCreatedEvent event = new AccountCreatedEvent(
            saved.getId(),
            saved.getCustomerId(),
            saved.getCustomerName(),
            saved.getEmail(),
            saved.getPhoneNumber(),
            saved.getCurrency(),
            saved.getBalance(),
            saved.getCreatedAt()
        );
        eventProducer.publishAccountCreated(event);

        return AccountResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(String id) {
        Account account = findAccountOrThrow(id);
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
            .map(AccountResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String id) {
        Account account = findAccountOrThrow(id);
        return account.getBalance();
    }

    @Override
    @Transactional
    public AccountResponse updateBalance(String id, BalanceOperationRequest request) {
        Account account = accountRepository.findByIdForUpdate(id)
            .orElseGet(() -> accountRepository.findByAccountNumberForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for update: " + id)));

        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance;
        String op = request.getOperationType().toUpperCase();

        if ("DEBIT".equals(op)) {
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException(String.format(
                    "Insufficient balance in account %s. Available: %s, Requested: %s",
                    account.getAccountNumber(), account.getBalance(), request.getAmount()
                ));
            }
            newBalance = account.getBalance().subtract(request.getAmount());
        } else if ("CREDIT".equals(op)) {
            newBalance = account.getBalance().add(request.getAmount());
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + op + ". Use CREDIT or DEBIT.");
        }

        account.setBalance(newBalance);
        Account updated = accountRepository.save(account);

        AccountTransaction txn = new AccountTransaction(
            request.getTransactionId(),
            updated.getId(),
            request.getAmount(),
            op,
            request.getDescription() != null ? request.getDescription() : (op + " operation"),
            request.getCounterpartyAccountId(),
            newBalance
        );
        transactionRepository.save(txn);

        log.info("Balance updated for account {}: {} -> {} ({} of {})", 
                 updated.getId(), previousBalance, newBalance, op, request.getAmount());

        BalanceUpdatedEvent event = new BalanceUpdatedEvent(
            updated.getId(),
            request.getTransactionId(),
            previousBalance,
            newBalance,
            request.getAmount(),
            op,
            Instant.now()
        );
        eventProducer.publishBalanceUpdated(event);

        return AccountResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountTransaction> getTransactionHistory(String id) {
        Account account = findAccountOrThrow(id);
        return transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountTransaction> getTransactionHistoryPaged(String id, Pageable pageable) {
        Account account = findAccountOrThrow(id);
        return transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId(), pageable);
    }

    private Account findAccountOrThrow(String id) {
        return accountRepository.findById(id)
            .orElseGet(() -> accountRepository.findByAccountNumber(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with identifier: " + id)));
    }

    private String generateAccountNumber() {
        long number = 1000000000L + (long)(random.nextDouble() * 9000000000L);
        return String.valueOf(number);
    }
}
