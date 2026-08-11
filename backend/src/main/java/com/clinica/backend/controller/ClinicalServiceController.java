package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.service.ClinicalServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/clinical-services", "/api/clinical-services"})
@RequiredArgsConstructor
public class ClinicalServiceController {

    private final ClinicalServiceService service;

    @GetMapping
    public ResponseEntity<Page<ClinicalServiceDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getAllServices(name, PageRequest.of(page, size)));
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
    public ResponseEntity<ClinicalServiceDto> create(@RequestBody ClinicalServiceDto dto) {
        return ResponseEntity.ok(service.createService(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClinicalServiceDto> update(@PathVariable Long id, @RequestBody ClinicalServiceDto dto) {
        return ResponseEntity.ok(service.updateService(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
