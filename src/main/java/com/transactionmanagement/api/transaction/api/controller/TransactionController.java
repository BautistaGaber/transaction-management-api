
package com.transactionmanagement.api.transaction.api.controller;

import com.transactionmanagement.api.transaction.api.dto.TransactionRequest;
import com.transactionmanagement.api.transaction.api.dto.TransactionStatusResponse;
import com.transactionmanagement.api.transaction.api.dto.TransactionSumResponse;
import com.transactionmanagement.api.transaction.api.mapper.TransactionMapper;
import com.transactionmanagement.api.transaction.application.service.TransactionServiceImpl;
import com.transactionmanagement.api.transaction.domain.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Crear o actualizar una transacción", description = """
        Almacena una transacción con un ID definido por el cliente.
        El parent_id es opcional, pero si se proporciona,
        la transacción padre debe existir.
        No se permiten relaciones circulares.
        """)
    @ApiResponses({@ApiResponse(responseCode = "200",
                    description = "Transacción almacenada correctamente"),
            @ApiResponse(responseCode = "400",
                    description = "Datos inválidos o relación no permitida")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TransactionStatusResponse> save(@PathVariable @Positive Long id, @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = mapper.toDomain(id, request);
        service.save(transaction);

        return ResponseEntity.ok(new TransactionStatusResponse("ok")
        );
    }


    @Operation(summary = "Obtener transacciones por tipo", description = "Devuelve los identificadores de todas las transacciones del tipo indicado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista de identificadores, posiblemente vacía")})
    @GetMapping("/types/{type}")
    public ResponseEntity<List<Long>> findByType(@PathVariable String type) {
        List<Long> ids = service.findIdsByType(type);
        return ResponseEntity.ok(ids);
    }


    @Operation(summary = "Calcular suma transitiva", description = """
        Calcula la suma del monto de una transacción
        y de todos sus descendientes directos e indirectos.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Suma calculada correctamente"),
            @ApiResponse(responseCode = "400",
                    description = "Identificador inválido"),
            @ApiResponse(responseCode = "404",
                    description = "Transacción inexistente"),
            @ApiResponse(responseCode = "422",
                    description = "El resultado supera el rango numérico admitido")
    })
    @GetMapping("/sum/{id}")
    public ResponseEntity<TransactionSumResponse> calculateSum(@PathVariable @Positive Long id) {
        double sum = service.calculateSum(id);

        return ResponseEntity.ok(new TransactionSumResponse(sum)
        );
    }
}
