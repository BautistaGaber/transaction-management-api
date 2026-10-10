package com.transactionmanagement.api.transaction.domain.repository;

import com.transactionmanagement.api.transaction.domain.model.Transaction;

import java.util.Collection;
import java.util.Optional;


public interface TransactionRepository {

    void save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    Collection<Transaction> findAll();
}
