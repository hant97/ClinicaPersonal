package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.service.ClinicalSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/clinical-sessions")
@RequiredArgsConstructor
public class ClinicalSessionController {

    private final ClinicalSessionService sessionService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<ClinicalSessionDto>> getSessionsByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sessionService.getSessionsByPatientId(patientId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalSessionDto> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @PostMapping
    public ResponseEntity<ClinicalSessionDto> createSession(@RequestBody ClinicalSessionDto dto) {
        return ResponseEntity.ok(sessionService.createSession(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicalSessionDto> updateSession(@PathVariable Long id,
            @RequestBody ClinicalSessionDto dto) {
        return ResponseEntity.ok(sessionService.updateSession(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
