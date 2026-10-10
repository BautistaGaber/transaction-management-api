package com.transactionmanagement.api.transaction.api.mapper;

import com.transactionmanagement.api.transaction.api.dto.TransactionRequest;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void shouldMapIdAmountTypeAndParentIdWhenParentIsProvided() {
        TransactionRequest request = new TransactionRequest(150.75, "food", 42L);

        Transaction transaction = mapper.toDomain(7L, request);

        assertAll(
                () -> assertEquals(7L, transaction.id()),
                () -> assertEquals(150.75, transaction.amount()),
                () -> assertEquals("food", transaction.type()),
                () -> assertEquals(42L, transaction.parentId())
        );
    }

    @Test
    void shouldMapNullParentIdWhenParentIsNotProvided() {
        TransactionRequest request = new TransactionRequest(10.0, "transport", null);

        Transaction transaction = mapper.toDomain(3L, request);

        assertAll(
                () -> assertEquals(3L, transaction.id()),
                () -> assertEquals(10.0, transaction.amount()),
                () -> assertEquals("transport", transaction.type()),
                () -> assertNull(transaction.parentId())
        );
    }

    @Test
    void shouldUseTheIdFromTheUrlInsteadOfAnyRequestBodyId() {
        TransactionRequest request = new TransactionRequest(20.0, "food", null);

        Transaction transaction = mapper.toDomain(999L, request);

        assertEquals(999L, transaction.id());
    }
}
