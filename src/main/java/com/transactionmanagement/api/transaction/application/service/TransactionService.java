package com.transactionmanagement.api.transaction.application.service;

import com.transactionmanagement.api.transaction.domain.model.Transaction;

import java.util.List;

public interface TransactionService {

    void save(Transaction transaction);

    List<Long> findIdsByType(String type);

    double calculateSum(Long transactionId);

}
