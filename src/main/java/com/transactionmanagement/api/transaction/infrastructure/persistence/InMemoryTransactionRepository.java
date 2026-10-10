package com.transactionmanagement.api.transaction.infrastructure.persistence;

import com.transactionmanagement.api.transaction.domain.model.Transaction;
import com.transactionmanagement.api.transaction.domain.repository.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final ConcurrentHashMap<Long, Transaction> transactions = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        transactions.put(transaction.id(), transaction);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public Collection<Transaction> findAll() {
        return List.copyOf(transactions.values());
    }
}
