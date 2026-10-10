
package com.transactionmanagement.api.transaction.application.service;

import com.transactionmanagement.api.shared.exception.TransactionNotFoundException;
import com.transactionmanagement.api.transaction.application.validation.TransactionValidator;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import com.transactionmanagement.api.transaction.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final TransactionValidator validator;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public TransactionService(TransactionRepository repository, TransactionValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public void save(Transaction transaction) {
        lock.writeLock().lock();

        try {
            validator.validate(transaction);
            repository.save(transaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Long> findIdsByType(String type) {
        lock.readLock().lock();

        try {
            return repository.findAll().stream()
                    .filter(transaction -> transaction.type().equals(type))
                    .map(Transaction::id)
                    .sorted()
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public double calculateSum(Long transactionId) {
        lock.readLock().lock();

        try {
            Transaction root = repository.findById(transactionId).orElseThrow(() -> new TransactionNotFoundException(transactionId));

            Map<Long, List<Transaction>> childrenByParent = new HashMap<>();

            for (Transaction transaction : repository.findAll()) {
                if (transaction.parentId() != null) {
                    childrenByParent.computeIfAbsent(transaction.parentId(), key -> new ArrayList<>()).add(transaction);
                }
            }

            double sum = 0.0;

            Deque<Transaction> pending = new ArrayDeque<>();
            Set<Long> visitedIds = new HashSet<>();

            pending.push(root);

            while (!pending.isEmpty()) {
                Transaction current = pending.pop();

                if (!visitedIds.add(current.id())) {
                    continue;
                }

                sum += current.amount();

                for (Transaction child : childrenByParent.getOrDefault(current.id(), List.of())) {
                    pending.push(child);
                }
            }

            return sum;
        } finally {
            lock.readLock().unlock();
        }
    }
}
