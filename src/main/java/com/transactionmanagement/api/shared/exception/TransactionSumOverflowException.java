package com.transactionmanagement.api.shared.exception;

public class TransactionSumOverflowException extends RuntimeException {
    public TransactionSumOverflowException(Long transactionId) {
        super("Transaction sum exceeds the supported numeric range: " + transactionId);
    }
}
