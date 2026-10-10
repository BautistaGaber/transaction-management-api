package com.transactionmanagement.api.transaction.infrastructure.persistence;

import com.transactionmanagement.api.transaction.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTransactionRepositoryTest {

    private static final String TYPE = "food";

    private InMemoryTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Test
    void shouldSaveAndFindTransactionById() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, null);

        repository.save(transaction);
        Optional<Transaction> result = repository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(transaction, result.get());
    }

    @Test
    void shouldReturnEmptyWhenIdDoesNotExist() {
        Optional<Transaction> result = repository.findById(123L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnAllSavedTransactions() {
        Transaction first = new Transaction(1L, 100.0, TYPE, null);
        Transaction second = new Transaction(2L, 50.0, "transport", 1L);
        repository.save(first);
        repository.save(second);

        Collection<Transaction> result = repository.findAll();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(first, second)));
    }

    @Test
    void shouldReplaceTransactionWhenSameIdIsSaved() {
        Transaction original = new Transaction(1L, 100.0, TYPE, null);
        Transaction replacement = new Transaction(1L, 250.0, "transport", null);
        repository.save(original);

        repository.save(replacement);

        Optional<Transaction> result = repository.findById(1L);
        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(replacement, result.get()),
                () -> assertEquals(250.0, result.get().amount())
        );
    }

    @Test
    void shouldReturnEmptyCollectionWhenRepositoryIsEmpty() {
        assertAll(
                () -> assertTrue(repository.findAll().isEmpty()),
                () -> assertTrue(repository.findById(1L).isEmpty())
        );
    }

    @Test
    void shouldReturnUnmodifiableCollectionFromFindAll() {
        repository.save(new Transaction(1L, 100.0, TYPE, null));
        Collection<Transaction> result = repository.findAll();

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> result.add(new Transaction(2L, 50.0, TYPE, null))),
                () -> assertThrows(UnsupportedOperationException.class, result::clear)
        );
    }
}
