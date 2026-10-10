package com.transactionmanagement.api.transaction.application.validation;

import com.transactionmanagement.api.transaction.domain.model.Transaction;
import com.transactionmanagement.api.transaction.domain.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TransactionValidator {

    private final TransactionRepository repository;

    public TransactionValidator(TransactionRepository repository) {
        this.repository = repository;
    }


    public void validate(Transaction transaction) {
        validateFields(transaction);
        validateParentRelationship(transaction);
    }

    private void validateFields(Transaction transaction) {
        if (transaction.id() == null || transaction.id() <= 0) {
            throw new IllegalArgumentException("Transaction id must be positive");
        }

        if (!Double.isFinite(transaction.amount())) {
            throw new IllegalArgumentException("Transaction amount must be finite");
        }

        if (transaction.type() == null || transaction.type().isBlank()) {
            throw new IllegalArgumentException("Transaction type must not be blank");
        }

        if (transaction.parentId() != null && transaction.parentId() <= 0) {
            throw new IllegalArgumentException("Parent id must be positive");
        }
    }

    private void validateParentRelationship(Transaction transaction) {
        Long ancestorId = transaction.parentId();
        Set<Long> visitedIds = new HashSet<>();

        while (ancestorId != null) {
            if (ancestorId.equals(transaction.id()) || !visitedIds.add(ancestorId)) {
                throw new IllegalArgumentException("Circular transaction relationship detected");
            }

            Long idToFind = ancestorId;

            Transaction ancestor = repository.findById(idToFind).orElseThrow(() -> new IllegalArgumentException("Parent transaction not found: " + idToFind));

            ancestorId = ancestor.parentId();
        }
    }

}
