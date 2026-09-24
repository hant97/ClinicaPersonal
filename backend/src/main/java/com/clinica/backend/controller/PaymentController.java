package com.clinica.backend.controller;

import com.clinica.backend.dto.PatientBalanceDto;
import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.dto.PaymentTransactionDto;
import com.clinica.backend.service.PaymentService;
import com.clinica.backend.service.PaymentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final PaymentReportService reportService;

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<PaymentDto>> getByPatientId(
            @PathVariable Long patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getByPatientId(patientId, pageable));
    }

    @GetMapping("/patient/{patientId}/balance")
    public ResponseEntity<PatientBalanceDto> getPatientBalance(@PathVariable Long patientId) {
        return ResponseEntity.ok(reportService.getPatientBalance(patientId));
    }

    @GetMapping("/summary")
    public ResponseEntity<PaymentSummaryDto> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getSummary(dateFrom, dateTo));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentDto>> getAll(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.getAll(searchTerm, dateFrom, dateTo, paymentMethod, status, pageable));
    }

    @PostMapping
    public ResponseEntity<PaymentDto> create(@Valid @RequestBody PaymentDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentDto> update(@PathVariable Long id, @Valid @RequestBody PaymentDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/{id}/transactions")
    public ResponseEntity<PaymentDto> addTransaction(@PathVariable Long id, @Valid @RequestBody PaymentTransactionDto dto) {
        return ResponseEntity.ok(service.addTransaction(id, dto));
    }

    @DeleteMapping("/{id}/transactions/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id, @PathVariable Long transactionId) {
        service.deleteTransaction(id, transactionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
