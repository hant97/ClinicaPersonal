package com.clinica.backend.controller;

import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.service.PsychometricTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping({"/api/v1/tests", "/api/tests"})
@RequiredArgsConstructor
public class PsychometricTestController {

    private final PsychometricTestService psychometricTestService;

    @GetMapping
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Page<PsychometricTestDto>> getAllTests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(psychometricTestService.getAllTests(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<PsychometricTestDto> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(psychometricTestService.getTestById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<PsychometricTestDto> createTest(@Valid @RequestBody PsychometricTestDto dto) {
        return ResponseEntity.ok(psychometricTestService.createTest(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<PsychometricTestDto> updateTest(@PathVariable Long id, @Valid @RequestBody PsychometricTestDto dto) {
        return ResponseEntity.ok(psychometricTestService.updateTest(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and principal.specialty == 'PSICOLOGIA'")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        psychometricTestService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }
}
