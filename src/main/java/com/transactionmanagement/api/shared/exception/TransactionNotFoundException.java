package com.transactionmanagement.api.shared.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found: " + transactionId);
    }
}
