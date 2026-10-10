package com.transactionmanagement.api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionSumIntegrationTest extends AbstractTransactionApiIntegrationTest {

    @Test
    void shouldSumTransactionWithoutChildren() throws Exception {
        save(1L, 100.0, "food", null);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(100.0));
    }

    @Test
    void shouldSumMultiLevelChain() throws Exception {
        save(1L, 10.0, "food", null);
        save(2L, 20.0, "food", 1L);
        save(3L, 30.0, "food", 2L);
        save(4L, 40.0, "food", 3L);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(100.0));
    }

    @Test
    void shouldSumTreeWithMultipleBranches() throws Exception {
        save(1L, 10.0, "food", null);
        save(2L, 5.0, "food", 1L);
        save(3L, 7.0, "food", 1L);
        save(4L, 3.0, "food", 2L);
        save(99L, 1000.0, "food", null);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(25.0));
    }

    @Test
    void shouldReflectParentChange() throws Exception {
        save(1L, 100.0, "food", null);
        save(2L, 50.0, "food", 1L);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(150.0));

        save(2L, 50.0, "food", null);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(100.0));

        mockMvc.perform(get("/transactions/sum/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(50.0));
    }

    @Test
    void shouldSumPositiveAndNegativeAmounts() throws Exception {
        save(1L, 100.0, "food", null);
        save(2L, -30.0, "food", 1L);
        save(3L, 5.0, "food", 1L);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(75.0));
    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotExist() throws Exception {
        mockMvc.perform(get("/transactions/sum/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnBadRequestWhenIdIsZero() throws Exception {
        mockMvc.perform(get("/transactions/sum/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnBadRequestWhenIdIsNegative() throws Exception {
        mockMvc.perform(get("/transactions/sum/-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnBadRequestWhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/transactions/sum/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnUnprocessableEntityWhenSumOverflows() throws Exception {
        save(1L, Double.MAX_VALUE, "food", null);
        save(2L, Double.MAX_VALUE, "food", 1L);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnSumInJsonResponse() throws Exception {
        save(1L, 12.5, "food", null);

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"sum\":12.5}"));
    }

    private void save(long id, double amount, String type, Long parentId) throws Exception {
        String body = (parentId == null) ? transactionBody(amount, type) : transactionBody(amount, type, parentId);
        mockMvc.perform(put("/transactions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
