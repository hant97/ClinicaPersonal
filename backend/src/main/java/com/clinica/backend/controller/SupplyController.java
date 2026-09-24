package com.clinica.backend.controller;

import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.dto.SupplyStatsDto;
import com.clinica.backend.service.SupplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/supplies")
@RequiredArgsConstructor
public class SupplyController {

    private final SupplyService supplyService;

    @GetMapping
    public ResponseEntity<Page<SupplyDto>> getAllSupplies(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(supplyService.getAllSupplies(name, pageable));
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<SupplyDto>> getLowStockSupplies() {
        return ResponseEntity.ok(supplyService.getLowStockSupplies());
    }

    @GetMapping("/stats")
    public ResponseEntity<SupplyStatsDto> getStats() {
        return ResponseEntity.ok(supplyService.getStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplyDto> getSupplyById(@PathVariable Long id) {
        return ResponseEntity.ok(supplyService.getSupplyById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupplyDto> createSupply(@Valid @RequestBody SupplyDto supplyDto) {
        return new ResponseEntity<>(supplyService.createSupply(supplyDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupplyDto> updateSupply(@PathVariable Long id, @Valid @RequestBody SupplyDto supplyDto) {
        return ResponseEntity.ok(supplyService.updateSupply(id, supplyDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSupply(@PathVariable Long id) {
        supplyService.deleteSupply(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupplyDto> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(supplyService.uploadImage(id, file));
    }
}
