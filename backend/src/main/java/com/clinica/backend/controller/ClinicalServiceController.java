package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.dto.ClinicalServiceStatsDto;
import com.clinica.backend.service.ClinicalServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clinical-services")
@RequiredArgsConstructor
public class ClinicalServiceController {

    private final ClinicalServiceService service;

    @GetMapping
    public ResponseEntity<Page<ClinicalServiceDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getAllServices(name, category, active, minPrice, maxPrice, PageRequest.of(page, size)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ClinicalServiceStatsDto> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ClinicalServiceDto>> getAllActive() {
        return ResponseEntity.ok(service.getAllActiveServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalServiceDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getServiceById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicalServiceDto> create(@Valid @RequestBody ClinicalServiceDto dto) {
        return ResponseEntity.ok(service.createService(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicalServiceDto> update(@PathVariable Long id, @Valid @RequestBody ClinicalServiceDto dto) {
        return ResponseEntity.ok(service.updateService(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
