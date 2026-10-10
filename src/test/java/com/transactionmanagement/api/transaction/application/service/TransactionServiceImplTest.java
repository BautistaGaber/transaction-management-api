package com.transactionmanagement.api.transaction.application.service;

import com.transactionmanagement.api.shared.exception.TransactionNotFoundException;
import com.transactionmanagement.api.shared.exception.TransactionSumOverflowException;
import com.transactionmanagement.api.transaction.application.validation.TransactionValidator;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import com.transactionmanagement.api.transaction.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final String TYPE = "food";
    private static final int DEEP_GRAPH_DEPTH = 50_000;

    @Mock
    private TransactionRepository repository;

    @Mock
    private TransactionValidator validator;

    @InjectMocks
    private TransactionServiceImpl service;

    // ---------------------------------------------------------------
    // Guardado
    // ---------------------------------------------------------------

    @Test
    void shouldSaveValidTransaction() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, null);

        service.save(transaction);

        verify(validator).validate(transaction);
        verify(repository).save(transaction);
    }

    @Test
    void shouldInvokeValidatorWhenSaving() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, null);

        service.save(transaction);

        verify(validator).validate(transaction);
    }

    @Test
    void shouldInvokeRepositorySaveWhenSaving() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, null);

        service.save(transaction);

        verify(repository).save(transaction);
    }

    @Test
    void shouldNotSaveWhenValidationFails() {
        Transaction transaction = new Transaction(1L, 100.0, TYPE, null);
        doThrow(new IllegalArgumentException("invalid"))
                .when(validator).validate(transaction);

        assertThrows(IllegalArgumentException.class, () -> service.save(transaction));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldReplaceExistingTransactionWhenSameIdIsSaved() {
        Transaction firstVersion = new Transaction(1L, 100.0, TYPE, null);
        Transaction secondVersion = new Transaction(1L, 250.0, TYPE, null);

        service.save(firstVersion);
        service.save(secondVersion);

        verify(validator).validate(firstVersion);
        verify(validator).validate(secondVersion);
        verify(repository).save(firstVersion);
        verify(repository).save(secondVersion);
    }

    // ---------------------------------------------------------------
    // Búsqueda por tipo
    // ---------------------------------------------------------------

    @Test
    void shouldReturnIdsForMatchingTypeWhenSingleMatch() {
        when(repository.findAll()).thenReturn(List.of(
                new Transaction(1L, 10.0, TYPE, null),
                new Transaction(2L, 20.0, "transport", null)
        ));

        List<Long> result = service.findIdsByType(TYPE);

        assertEquals(List.of(1L), result);
    }

    @Test
    void shouldReturnAllIdsWhenMultipleTransactionsMatch() {
        when(repository.findAll()).thenReturn(List.of(
                new Transaction(1L, 10.0, TYPE, null),
                new Transaction(2L, 20.0, TYPE, null),
                new Transaction(3L, 30.0, TYPE, null)
        ));

        List<Long> result = service.findIdsByType(TYPE);

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void shouldReturnEmptyListWhenNoTransactionMatches() {
        when(repository.findAll()).thenReturn(List.of(
                new Transaction(1L, 10.0, "transport", null),
                new Transaction(2L, 20.0, "housing", null)
        ));

        List<Long> result = service.findIdsByType(TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnOnlyIdsOfTheRequestedType() {
        when(repository.findAll()).thenReturn(List.of(
                new Transaction(1L, 10.0, TYPE, null),
                new Transaction(2L, 20.0, "transport", null),
                new Transaction(3L, 30.0, TYPE, null)
        ));

        List<Long> result = service.findIdsByType(TYPE);

        assertEquals(List.of(1L, 3L), result);
    }

    @Test
    void shouldReturnIdsInAscendingOrder() {
        when(repository.findAll()).thenReturn(List.of(
                new Transaction(30L, 10.0, TYPE, null),
                new Transaction(5L, 20.0, TYPE, null),
                new Transaction(12L, 30.0, TYPE, null)
        ));

        List<Long> result = service.findIdsByType(TYPE);

        assertEquals(List.of(5L, 12L, 30L), result);
    }

    @Test
    void shouldMatchTypeCaseSensitively() {
        when(repository.findAll()).thenReturn(List.of(
                new Transaction(1L, 10.0, "FOOD", null),
                new Transaction(2L, 20.0, "food", null)
        ));

        List<Long> lowerCase = service.findIdsByType("food");
        List<Long> upperCase = service.findIdsByType("FOOD");

        assertEquals(List.of(2L), lowerCase);
        assertEquals(List.of(1L), upperCase);
    }

    // ---------------------------------------------------------------
    // Suma transitiva
    // ---------------------------------------------------------------

    @Test
    void shouldReturnOwnAmountWhenTransactionHasNoChildren() {
        Transaction root = new Transaction(1L, 100.0, TYPE, null);
        givenRoot(1L, root);
        when(repository.findAll()).thenReturn(List.of(root));

        double sum = service.calculateSum(1L);

        assertEquals(100.0, sum);
    }

    @Test
    void shouldSumSingleChild() {
        Transaction root = new Transaction(1L, 100.0, TYPE, null);
        Transaction child = new Transaction(2L, 50.0, TYPE, 1L);
        givenRoot(1L, root);
        when(repository.findAll()).thenReturn(List.of(root, child));

        double sum = service.calculateSum(1L);

        assertEquals(150.0, sum);
    }

    @Test
    void shouldSumMultiLevelChain() {
        Transaction levelOne = new Transaction(1L, 10.0, TYPE, null);
        Transaction levelTwo = new Transaction(2L, 20.0, TYPE, 1L);
        Transaction levelThree = new Transaction(3L, 30.0, TYPE, 2L);
        Transaction levelFour = new Transaction(4L, 40.0, TYPE, 3L);
        givenRoot(1L, levelOne);
        when(repository.findAll()).thenReturn(List.of(levelOne, levelTwo, levelThree, levelFour));

        double sum = service.calculateSum(1L);

        assertEquals(100.0, sum);
    }

    @Test
    void shouldSumAllBranchesOfTree() {
        Transaction root = new Transaction(1L, 10.0, TYPE, null);
        Transaction branchA = new Transaction(2L, 5.0, TYPE, 1L);
        Transaction branchB = new Transaction(3L, 7.0, TYPE, 1L);
        Transaction branchAContinues = new Transaction(4L, 3.0, TYPE, 2L);
        Transaction unrelated = new Transaction(99L, 1000.0, TYPE, null);
        givenRoot(1L, root);
        when(repository.findAll()).thenReturn(List.of(root, branchA, branchB, branchAContinues, unrelated));

        double sum = service.calculateSum(1L);

        assertEquals(25.0, sum);
    }

    @Test
    void shouldSumPositiveAndNegativeAmounts() {
        Transaction root = new Transaction(1L, 100.0, TYPE, null);
        Transaction child = new Transaction(2L, -30.0, TYPE, 1L);
        givenRoot(1L, root);
        when(repository.findAll()).thenReturn(List.of(root, child));

        double sum = service.calculateSum(1L);

        assertEquals(70.0, sum);
    }

    @Test
    void shouldReturnZeroWhenAmountsCancelOut() {
        Transaction root = new Transaction(1L, 10.0, TYPE, null);
        Transaction child = new Transaction(2L, -10.0, TYPE, 1L);
        givenRoot(1L, root);
        when(repository.findAll()).thenReturn(List.of(root, child));

        double sum = service.calculateSum(1L);

        assertEquals(0.0, sum);
    }

    @Test
    void shouldThrowWhenTransactionDoesNotExist() {
        assertThrows(TransactionNotFoundException.class, () -> service.calculateSum(99L));
    }

    @Test
    void shouldTraverseDeepGraphIterativelyWithoutStackOverflow() {
        List<Transaction> graph = new ArrayList<>(DEEP_GRAPH_DEPTH);
        graph.add(new Transaction(1L, 1.0, TYPE, null));
        for (long id = 2; id <= DEEP_GRAPH_DEPTH; id++) {
            graph.add(new Transaction(id, 1.0, TYPE, id - 1));
        }
        givenRoot(1L, graph.get(0));
        when(repository.findAll()).thenReturn(graph);

        double sum = service.calculateSum(1L);

        assertEquals((double) DEEP_GRAPH_DEPTH, sum);
    }

    @Test
    void shouldCountEachVisitedNodeOnceWhenCycleExists() {
        Transaction first = new Transaction(1L, 10.0, TYPE, 2L);
        Transaction second = new Transaction(2L, 5.0, TYPE, 1L);
        givenRoot(1L, first);
        when(repository.findAll()).thenReturn(List.of(first, second));

        double sum = service.calculateSum(1L);

        assertEquals(15.0, sum);
    }

    @Test
    void shouldCountSelfReferencingNodeOnlyOnce() {
        Transaction selfReferencing = new Transaction(1L, 10.0, TYPE, 1L);
        givenRoot(1L, selfReferencing);
        when(repository.findAll()).thenReturn(List.of(selfReferencing));

        double sum = service.calculateSum(1L);

        assertEquals(10.0, sum);
    }

    @Test
    void shouldThrowWhenSumOverflows() {
        Transaction root = new Transaction(1L, Double.MAX_VALUE, TYPE, null);
        Transaction child = new Transaction(2L, Double.MAX_VALUE, TYPE, 1L);
        givenRoot(1L, root);
        when(repository.findAll()).thenReturn(List.of(root, child));

        assertThrows(TransactionSumOverflowException.class, () -> service.calculateSum(1L));
    }

    private void givenRoot(Long id, Transaction root) {
        when(repository.findById(id)).thenReturn(Optional.of(root));
    }
}
