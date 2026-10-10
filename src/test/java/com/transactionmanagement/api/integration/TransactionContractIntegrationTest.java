package com.transactionmanagement.api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionContractIntegrationTest extends AbstractTransactionApiIntegrationTest {

    @Test
    void shouldReturnExactStatusOkBodyForPut() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(aMapWithSize(1)))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldReturnExactSumBodyForSumEndpoint() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(aMapWithSize(1)))
                .andExpect(jsonPath("$.sum").value(100.0));
    }

    @Test
    void shouldReturnExactErrorBodyForControlledErrors() throws Exception {
        mockMvc.perform(get("/transactions/sum/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(aMapWithSize(1)))
                .andExpect(jsonPath("$").value(hasKey("error")))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void shouldReturnErrorBodyForValidationErrors() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"food\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value(hasKey("error")))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void shouldBindSnakeCaseParentIdField() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/transactions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(50.0, "food", 1L)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(150.0));
    }

    @Test
    void shouldIgnoreCamelCaseParentIdField() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/transactions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.0,\"type\":\"food\",\"parentId\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(100.0));

        mockMvc.perform(get("/transactions/sum/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(50.0));
    }
}
