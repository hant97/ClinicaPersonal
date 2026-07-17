package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.service.ClinicalServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clinical-services")
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
    public ResponseEntity<ClinicalServiceDto> create(@RequestBody ClinicalServiceDto dto) {
        return ResponseEntity.ok(service.createService(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicalServiceDto> update(@PathVariable Long id, @RequestBody ClinicalServiceDto dto) {
        return ResponseEntity.ok(service.updateService(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
