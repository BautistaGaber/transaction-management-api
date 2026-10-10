package com.transactionmanagement.api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionTypesIntegrationTest extends AbstractTransactionApiIntegrationTest {

    @Test
    void shouldReturnIdsForExistingType() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnMultipleIds() throws Exception {
        save(1L, 100.0, "food");
        save(2L, 200.0, "food");
        save(3L, 300.0, "food");

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(2))
                .andExpect(jsonPath("$[2]").value(3));
    }

    @Test
    void shouldReturnIdsInAscendingOrder() throws Exception {
        save(30L, 100.0, "food");
        save(5L, 100.0, "food");
        save(12L, 100.0, "food");

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(5))
                .andExpect(jsonPath("$[1]").value(12))
                .andExpect(jsonPath("$[2]").value(30));
    }

    @Test
    void shouldReturnEmptyListForUnknownType() throws Exception {
        save(1L, 100.0, "food");

        mockMvc.perform(get("/transactions/types/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldMatchTypeCaseSensitively() throws Exception {
        save(1L, 100.0, "FOOD");
        save(2L, 100.0, "food");

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(2));

        mockMvc.perform(get("/transactions/types/FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void shouldReflectUpdatedType() throws Exception {
        save(1L, 100.0, "food");

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "transport")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/transactions/types/transport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(1));
    }

    private void save(long id, double amount, String type) throws Exception {
        mockMvc.perform(put("/transactions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(amount, type)))
                .andExpect(status().isOk());
    }
}
