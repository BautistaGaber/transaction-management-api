package com.transactionmanagement.api.transaction.application.validation;

import com.transactionmanagement.api.transaction.domain.model.Transaction;
import com.transactionmanagement.api.transaction.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionValidatorTest {

    private static final String TYPE = "food";

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionValidator validator;

    @Test
    void shouldNotThrowWhenTransactionIsValidWithoutParent() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, null);

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    void shouldNotThrowWhenTransactionIsValidWithExistingParent() {
        Transaction parent = new Transaction(1L, 50.0, TYPE, null);
        Transaction transaction = new Transaction(2L, 100.0, TYPE, 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(parent));

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    void shouldThrowWhenIdIsNull() {
        Transaction transaction = new Transaction(null, 100.0, TYPE, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction id must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowWhenIdIsZero() {
        Transaction transaction = new Transaction(0L, 100.0, TYPE, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction id must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowWhenIdIsNegative() {
        Transaction transaction = new Transaction(-1L, 100.0, TYPE, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction id must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAmountIsNaN() {
        Transaction transaction = new Transaction(1L, Double.NaN, TYPE, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction amount must be finite", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAmountIsPositiveInfinity() {
        Transaction transaction = new Transaction(1L, Double.POSITIVE_INFINITY, TYPE, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction amount must be finite", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAmountIsNegativeInfinity() {
        Transaction transaction = new Transaction(1L, Double.NEGATIVE_INFINITY, TYPE, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction amount must be finite", exception.getMessage());
    }

    @Test
    void shouldNotThrowWhenAmountIsNegative() {
        Transaction transaction = new Transaction(1L, -75.5, TYPE, null);

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    void shouldNotThrowWhenAmountIsZero() {
        Transaction transaction = new Transaction(1L, 0.0, TYPE, null);

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    void shouldThrowWhenTypeIsNull() {
        Transaction transaction = new Transaction(1L, 100.0, null, null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction type must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowWhenTypeIsEmpty() {
        Transaction transaction = new Transaction(1L, 100.0, "", null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction type must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowWhenTypeIsBlank() {
        Transaction transaction = new Transaction(1L, 100.0, "   ", null);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Transaction type must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowWhenParentIdIsZero() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, 0L);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Parent id must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowWhenParentIdIsNegative() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, -3L);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Parent id must be positive", exception.getMessage());
    }

    @Test
    void shouldThrowWhenParentDoesNotExist() {
        Transaction transaction = new Transaction(2L, 100.0, TYPE, 99L);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void shouldThrowWhenTransactionReferencesItself() {
        Transaction transaction = new Transaction(5L, 100.0, TYPE, 5L);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(transaction));

        assertEquals("Circular transaction relationship detected", exception.getMessage());
    }

    @Test
    void shouldThrowWhenIndirectCycleExists() {
        Transaction newTransaction = new Transaction(3L, 100.0, TYPE, 2L);
        Transaction ancestorTwo = new Transaction(2L, 10.0, TYPE, 1L);
        Transaction ancestorOne = new Transaction(1L, 10.0, TYPE, 3L);
        when(repository.findById(2L)).thenReturn(Optional.of(ancestorTwo));
        when(repository.findById(1L)).thenReturn(Optional.of(ancestorOne));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(newTransaction));

        assertEquals("Circular transaction relationship detected", exception.getMessage());
    }

    @Test
    void shouldThrowWhenExistingAncestorsFormACycle() {
        Transaction newTransaction = new Transaction(10L, 100.0, TYPE, 2L);
        Transaction ancestorTwo = new Transaction(2L, 10.0, TYPE, 1L);
        Transaction ancestorOne = new Transaction(1L, 10.0, TYPE, 2L);
        when(repository.findById(2L)).thenReturn(Optional.of(ancestorTwo));
        when(repository.findById(1L)).thenReturn(Optional.of(ancestorOne));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(newTransaction));

        assertEquals("Circular transaction relationship detected", exception.getMessage());
    }

    @Test
    void shouldNotThrowWhenAncestorChainIsValid() {
        Transaction newTransaction = new Transaction(4L, 100.0, TYPE, 3L);
        Transaction ancestorThree = new Transaction(3L, 10.0, TYPE, 2L);
        Transaction ancestorTwo = new Transaction(2L, 10.0, TYPE, 1L);
        Transaction ancestorOne = new Transaction(1L, 10.0, TYPE, null);
        when(repository.findById(3L)).thenReturn(Optional.of(ancestorThree));
        when(repository.findById(2L)).thenReturn(Optional.of(ancestorTwo));
        when(repository.findById(1L)).thenReturn(Optional.of(ancestorOne));

        assertDoesNotThrow(() -> validator.validate(newTransaction));
    }
}
