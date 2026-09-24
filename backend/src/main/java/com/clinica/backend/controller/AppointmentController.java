package com.clinica.backend.controller;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.dto.UpdateAppointmentStatusRequest;
import com.clinica.backend.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService service;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<AppointmentDto>> getByPatientId(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getByPatientId(patientId, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentDto>> getAll(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AppointmentDto>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity
                .ok(service.searchAppointments(searchTerm, status, professionalId, startDate, endDate, pageable));
    }

    @PostMapping
    public ResponseEntity<AppointmentDto> create(@Valid @RequestBody AppointmentDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDto> update(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentDto dto,
            @RequestParam(defaultValue = "false") boolean updateSeries) {
        return ResponseEntity.ok(service.update(id, dto, updateSeries));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest payload,
            @RequestParam(defaultValue = "false") boolean updateSeries) {
        return ResponseEntity.ok(service.updateStatus(id, payload.getStatus(), updateSeries));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AppointmentDto> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean cancelSeries) {
        return ResponseEntity.ok(service.updateStatus(id, "CANCELADA", cancelSeries));
    }
}
