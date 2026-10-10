package com.transactionmanagement.api.integration;

import com.transactionmanagement.api.transaction.application.service.TransactionService;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionConcurrencyIntegrationTest extends AbstractTransactionApiIntegrationTest {

    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private TransactionService service;

    @Test
    void shouldPersistAllTransactionsWhenSavedConcurrently() throws Exception {
        int totalTransactions = 100;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (long id = 1; id <= totalTransactions; id++) {
            long transactionId = id;
            futures.add(pool.submit(() -> {
                start.await();
                service.save(new Transaction(transactionId, 1.0, "food", null));
                return null;
            }));
        }

        start.countDown();
        awaitAll(futures);
        shutdown(pool);

        assertEquals(totalTransactions, repository.findAll().size());
        assertEquals(totalTransactions, service.findIdsByType("food").size());
        assertNoCycles();
    }

    @Test
    void shouldPreventCycleWhenConcurrentUpdatesWouldCreateOne() throws Exception {
        service.save(new Transaction(1L, 10.0, "food", null));
        service.save(new Transaction(2L, 10.0, "food", null));

        AtomicInteger successes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<?> first = pool.submit(() -> {
            start.await();
            try {
                service.save(new Transaction(1L, 10.0, "food", 2L));
                successes.incrementAndGet();
            } catch (IllegalArgumentException expected) {
                // cycle must be rejected
            }
            return null;
        });
        Future<?> second = pool.submit(() -> {
            start.await();
            try {
                service.save(new Transaction(2L, 10.0, "food", 1L));
                successes.incrementAndGet();
            } catch (IllegalArgumentException expected) {
                // cycle must be rejected
            }
            return null;
        });

        start.countDown();
        awaitAll(List.of(first, second));
        shutdown(pool);

        assertEquals(1, successes.get());
        assertNoCycles();

        double sum = service.calculateSum(1L);
        assertTrue(Double.isFinite(sum));
    }

    @Test
    void shouldReturnOnlyConsistentValuesWhenReadsRaceWithWrites() throws Exception {
        service.save(new Transaction(1L, 100.0, "food", null));

        int readers = 20;
        List<Double> results = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < readers; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                results.add(service.calculateSum(1L));
                return null;
            }));
        }
        futures.add(pool.submit(() -> {
            start.await();
            service.save(new Transaction(1L, 200.0, "food", null));
            return null;
        }));

        start.countDown();
        awaitAll(futures);
        shutdown(pool);

        assertEquals(readers, results.size());
        assertTrue(results.stream().allMatch(value -> value == 100.0 || value == 200.0),
                "Reads must observe either the old or the new amount, never a torn value");
        assertNoCycles();
    }

    private void assertNoCycles() {
        Collection<Transaction> transactions = repository.findAll();
        for (Transaction transaction : transactions) {
            Set<Long> visited = new HashSet<>();
            Long parentId = transaction.parentId();
            while (parentId != null) {
                assertTrue(visited.add(parentId),
                        "Detected a cycle involving transaction id: " + parentId);
                Transaction parent = repository.findById(parentId).orElse(null);
                if (parent == null) {
                    break;
                }
                parentId = parent.parentId();
            }
        }
    }

    private void awaitAll(List<Future<?>> futures) throws Exception {
        for (Future<?> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        assertTrue(pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
}
