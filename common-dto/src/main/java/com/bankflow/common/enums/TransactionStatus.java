package com.bankflow.common.enums;

public enum TransactionStatus {
    INITIATED,
    PENDING_FRAUD_CHECK,
    APPROVED,
    REJECTED,
    FRAUD_FLAGGED,
    COMPLETED,
    FAILED,
    COMPENSATED
}
