package com.bankflow.account.service;

import com.bankflow.account.dto.AccountResponse;
import com.bankflow.account.dto.CreateAccountRequest;
import com.bankflow.account.event.AccountEventProducer;
import com.bankflow.account.exception.AccountNotFoundException;
import com.bankflow.account.exception.InsufficientBalanceException;
import com.bankflow.account.model.Account;
import com.bankflow.account.model.AccountTransaction;
import com.bankflow.account.repository.AccountRepository;
import com.bankflow.account.repository.AccountTransactionRepository;
import com.bankflow.common.dto.BalanceOperationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository transactionRepository;

    @Mock
    private AccountEventProducer eventProducer;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, transactionRepository, eventProducer);
    }

    @Test
    @DisplayName("createAccount: successfully creates account and publishes event")
    void testCreateAccountSuccess() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setCustomerId("cust-1");
        req.setCustomerName("Alice");
        req.setEmail("alice@example.com");
        req.setPhoneNumber("+919999999999");
        req.setCurrency("INR");
        req.setInitialBalance(new BigDecimal("5000.00"));

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse resp = accountService.createAccount(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getCustomerId()).isEqualTo("cust-1");
        assertThat(resp.getBalance()).isEqualByComparingTo("5000.00");
        verify(transactionRepository, times(1)).save(any(AccountTransaction.class));
        verify(eventProducer, times(1)).publishAccountCreated(any());
    }

    @Test
    @DisplayName("getAccountById: throws AccountNotFoundException if missing")
    void testGetAccountByIdNotFound() {
        when(accountRepository.findById("missing-acc")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("missing-acc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById("missing-acc"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("updateBalance: successfully credits account")
    void testUpdateBalanceCredit() {
        Account acc = new Account("acc-1", "1234567890", "cust-1", "Alice", "alice@example.com", "+919999999999", "INR", new BigDecimal("1000.00"));
        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        BalanceOperationRequest req = new BalanceOperationRequest("txn-1", new BigDecimal("500.00"), "CREDIT", "Gift", "acc-2");
        AccountResponse resp = accountService.updateBalance("acc-1", req);

        assertThat(resp.getBalance()).isEqualByComparingTo("1500.00");
        verify(eventProducer, times(1)).publishBalanceUpdated(any());
        verify(transactionRepository, times(1)).save(any(AccountTransaction.class));
    }

    @Test
    @DisplayName("updateBalance: throws InsufficientBalanceException when debit exceeds balance")
    void testUpdateBalanceInsufficient() {
        Account acc = new Account("acc-1", "1234567890", "cust-1", "Alice", "alice@example.com", "+919999999999", "INR", new BigDecimal("200.00"));
        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(acc));

        BalanceOperationRequest req = new BalanceOperationRequest("txn-1", new BigDecimal("500.00"), "DEBIT", "Bill", "acc-2");

        assertThatThrownBy(() -> accountService.updateBalance("acc-1", req))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("updateBalance: throws IllegalArgumentException for unsupported operation")
    void testUpdateBalanceInvalidOp() {
        Account acc = new Account("acc-1", "1234567890", "cust-1", "Alice", "alice@example.com", "+919999999999", "INR", new BigDecimal("200.00"));
        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(acc));

        BalanceOperationRequest req = new BalanceOperationRequest("txn-1", new BigDecimal("100.00"), "MULTIPLY", "Invalid", "acc-2");

        assertThatThrownBy(() -> accountService.updateBalance("acc-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported operation type");
    }
}
