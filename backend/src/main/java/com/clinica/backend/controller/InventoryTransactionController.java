package com.clinica.backend.controller;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService transactionService;

    @PostMapping
    public ResponseEntity<InventoryTransactionDto> recordTransaction(@RequestBody InventoryTransactionDto dto) {
        return ResponseEntity.ok(transactionService.recordTransaction(dto));
    }

    @GetMapping("/supply/{supplyId}")
    public ResponseEntity<List<InventoryTransactionDto>> getTransactionsBySupply(@PathVariable Long supplyId) {
        return ResponseEntity.ok(transactionService.getTransactionsBySupply(supplyId));
    }
}
