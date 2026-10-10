
package com.transactionmanagement.api.transaction.api.mapper;

import com.transactionmanagement.api.transaction.api.dto.TransactionRequest;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toDomain(Long id, TransactionRequest request) {
        return new Transaction(id, request.amount(), request.type(), request.parentId());
    }
}
