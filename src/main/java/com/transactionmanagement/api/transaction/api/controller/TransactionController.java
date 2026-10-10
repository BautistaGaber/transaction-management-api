
package com.transactionmanagement.api.transaction.api.controller;

import com.transactionmanagement.api.transaction.api.dto.TransactionRequest;
import com.transactionmanagement.api.transaction.api.dto.TransactionStatusResponse;
import com.transactionmanagement.api.transaction.api.dto.TransactionSumResponse;
import com.transactionmanagement.api.transaction.api.mapper.TransactionMapper;
import com.transactionmanagement.api.transaction.application.service.TransactionServiceImpl;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionServiceImpl service;
    private final TransactionMapper mapper;

    public TransactionController(TransactionServiceImpl service, TransactionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionStatusResponse> save(@PathVariable @Positive Long id, @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = mapper.toDomain(id, request);
        service.save(transaction);

        return ResponseEntity.ok(new TransactionStatusResponse("ok")
        );
    }

    @GetMapping("/types/{type}")
    public ResponseEntity<List<Long>> findByType(@PathVariable String type) {
        List<Long> ids = service.findIdsByType(type);
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/sum/{id}")
    public ResponseEntity<TransactionSumResponse> calculateSum(@PathVariable @Positive Long id) {
        double sum = service.calculateSum(id);

        return ResponseEntity.ok(new TransactionSumResponse(sum)
        );
    }
}
