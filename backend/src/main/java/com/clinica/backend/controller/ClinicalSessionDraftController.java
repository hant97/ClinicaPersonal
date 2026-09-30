package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalSessionDraftRequest;
import com.clinica.backend.dto.ClinicalSessionDraftResponse;
import com.clinica.backend.service.ClinicalSessionDraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinical-drafts/patients/{patientId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROFESIONAL')")
public class ClinicalSessionDraftController {

    private final ClinicalSessionDraftService draftService;

    @GetMapping
    public ResponseEntity<ClinicalSessionDraftResponse> getDraft(@PathVariable Long patientId) {
        return draftService.getDraft(patientId)
                .map(draft -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(draft))
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    @PutMapping
    public ResponseEntity<ClinicalSessionDraftResponse> saveDraft(
            @PathVariable Long patientId,
            @Valid @RequestBody ClinicalSessionDraftRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(draftService.saveDraft(patientId, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteDraft(@PathVariable Long patientId, @RequestParam Long version) {
        draftService.deleteDraft(patientId, version);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
