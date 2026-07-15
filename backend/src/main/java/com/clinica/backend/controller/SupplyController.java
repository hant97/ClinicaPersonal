package com.clinica.backend.controller;

import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.service.SupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplies")
@RequiredArgsConstructor
public class SupplyController {

    private final SupplyService supplyService;

    @GetMapping
    public ResponseEntity<List<SupplyDto>> getAllSupplies() {
        return ResponseEntity.ok(supplyService.getAllSupplies());
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<SupplyDto>> getLowStockSupplies() {
        return ResponseEntity.ok(supplyService.getLowStockSupplies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplyDto> getSupplyById(@PathVariable Long id) {
        return ResponseEntity.ok(supplyService.getSupplyById(id));
    }

    @PostMapping
    public ResponseEntity<SupplyDto> createSupply(@RequestBody SupplyDto supplyDto) {
        return new ResponseEntity<>(supplyService.createSupply(supplyDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplyDto> updateSupply(@PathVariable Long id, @RequestBody SupplyDto supplyDto) {
        return ResponseEntity.ok(supplyService.updateSupply(id, supplyDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupply(@PathVariable Long id) {
        supplyService.deleteSupply(id);
        return ResponseEntity.noContent().build();
    }
}
