package com.clinica.backend.controller;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.dto.AttentionSummaryDto;
import com.clinica.backend.dto.UpdateAttentionStatusRequest;
import com.clinica.backend.service.AttentionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attentions")
@RequiredArgsConstructor
public class AttentionController {

    private final AttentionService service;

    @GetMapping
    public ResponseEntity<Page<AttentionDto>> getAll(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.searchAttentions(
                searchTerm, status, professionalId, patientId, startDate, endDate, PageRequest.of(page, size)
        ));
    }

    @GetMapping("/today-summary")
    public ResponseEntity<AttentionSummaryDto> getTodaySummary() {
        return ResponseEntity.ok(service.getTodaySummary());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttentionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAttentionById(id));
    }

    @PostMapping
    public ResponseEntity<AttentionDto> create(@Valid @RequestBody AttentionDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PostMapping("/from-appointment/{appointmentId}")
    public ResponseEntity<AttentionDto> createFromAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(service.createFromAppointment(appointmentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttentionDto> update(@PathVariable Long id, @Valid @RequestBody AttentionDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AttentionDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttentionStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request.getStatus(), request.getNotes()));
    }

    @PostMapping("/{id}/link-session/{sessionId}")
    public ResponseEntity<AttentionDto> linkSession(
            @PathVariable Long id,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(service.linkClinicalSession(id, sessionId));
    }

    @PostMapping("/{id}/link-prescription/{prescriptionId}")
    public ResponseEntity<AttentionDto> linkPrescription(
            @PathVariable Long id,
            @PathVariable Long prescriptionId) {
        return ResponseEntity.ok(service.linkPrescription(id, prescriptionId));
    }

    @PostMapping("/{id}/link-payment/{paymentId}")
    public ResponseEntity<AttentionDto> linkPayment(
            @PathVariable Long id,
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(service.linkPayment(id, paymentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
