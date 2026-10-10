package com.transactionmanagement.api.integration;

import com.transactionmanagement.api.transaction.infrastructure.persistence.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractTransactionApiIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected InMemoryTransactionRepository repository;

    @BeforeEach
    void resetRepository() {
        Map<Long, ?> transactions =
                (Map<Long, ?>) ReflectionTestUtils.getField(repository, "transactions");
        if (transactions != null) {
            transactions.clear();
        }
    }

    protected String transactionBody(double amount, String type) {
        return "{\"amount\":" + amount + ",\"type\":\"" + type + "\"}";
    }

    protected String transactionBody(double amount, String type, long parentId) {
        return "{\"amount\":" + amount + ",\"type\":\"" + type + "\",\"parent_id\":" + parentId + "}";
    }
}
