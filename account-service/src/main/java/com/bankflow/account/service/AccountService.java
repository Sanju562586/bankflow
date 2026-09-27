package com.bankflow.account.service;

import com.bankflow.account.dto.AccountResponse;
import com.bankflow.common.dto.BalanceOperationRequest;
import com.bankflow.account.dto.CreateAccountRequest;
import com.bankflow.account.model.Account;
import com.bankflow.account.model.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);
    AccountResponse getAccountById(String id);
    AccountResponse getAccountByAccountNumber(String accountNumber);
    List<AccountResponse> getAllAccounts();
    BigDecimal getBalance(String id);
    AccountResponse updateBalance(String id, BalanceOperationRequest request);
    List<AccountTransaction> getTransactionHistory(String id);
    Page<AccountTransaction> getTransactionHistoryPaged(String id, Pageable pageable);
}
