package com.clinica.backend.controller;

import com.clinica.backend.dto.AuditLogDto;
import com.clinica.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SITE_ADMIN') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SITE_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(
                startDate,
                endDate,
                username,
                specialty,
                action,
                entityType,
                query,
                pageable
        ));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<String>> getDistinctActions() {
        return ResponseEntity.ok(auditLogService.getDistinctActions());
    }

    @GetMapping("/entity-types")
    public ResponseEntity<List<String>> getDistinctEntityTypes() {
        return ResponseEntity.ok(auditLogService.getDistinctEntityTypes());
    }
}
