package com.transactionmanagement.api.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionPutIntegrationTest extends AbstractTransactionApiIntegrationTest {

    @Test
    void shouldCreateValidTransactionWithoutParent() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void shouldCreateTransactionWithExistingParent() throws Exception {
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
    void shouldUpdateExistingTransaction() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(250.0, "transport")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/transactions/types/transport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void shouldBeIdempotentWhenSamePutIsRepeated() throws Exception {
        String body = transactionBody(100.0, "food");

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/types/food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void shouldRejectZeroId() throws Exception {
        mockMvc.perform(put("/transactions/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectNegativeId() throws Exception {
        mockMvc.perform(put("/transactions/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectNonNumericId() throws Exception {
        mockMvc.perform(put("/transactions/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"food\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectNonNumericAmount() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"abc\",\"type\":\"food\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectBlankType() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.0,\"type\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.0,\"type\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectNonExistingParent() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food", 99L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectNonPositiveParentId() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food", 0L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectSelfReference() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectIndirectCycle() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/transactions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(50.0, "food", 1L)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/transactions/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(25.0, "food", 2L)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food", 3L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldNotModifyExistingDataWhenOperationIsRejected() throws Exception {
        mockMvc.perform(put("/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(100.0, "food")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/transactions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(10.0, "food", 99L)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/transactions/sum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sum").value(100.0));

        mockMvc.perform(get("/transactions/sum/2"))
                .andExpect(status().isNotFound());
    }
}
