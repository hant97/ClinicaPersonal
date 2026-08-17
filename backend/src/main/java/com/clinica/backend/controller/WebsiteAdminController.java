package com.clinica.backend.controller;

import com.clinica.backend.dto.WebsiteDraftDto;
import com.clinica.backend.dto.WebsiteEditorDto;
import com.clinica.backend.service.WebsiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/website")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SITE_ADMIN', 'ADMIN')")
public class WebsiteAdminController {
    private final WebsiteSettingsService service;

    @GetMapping("/editor")
    public ResponseEntity<WebsiteEditorDto> getEditor() {
        return ResponseEntity.ok(service.getEditor());
    }

    @PutMapping("/draft")
    public ResponseEntity<WebsiteEditorDto> saveDraft(@RequestBody WebsiteDraftDto dto, @RequestParam long revision) {
        return ResponseEntity.ok(service.saveDraft(dto, revision));
    }

    @PostMapping("/publish")
    public ResponseEntity<WebsiteEditorDto> publish(@RequestParam long revision) {
        return ResponseEntity.ok(service.publish(revision));
    }

    @PostMapping("/draft/reset")
    public ResponseEntity<WebsiteEditorDto> resetDraft(@RequestParam long revision) {
        return ResponseEntity.ok(service.resetDraft(revision));
    }

    @PostMapping(value = "/draft/assets/{category}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebsiteEditorDto> uploadDraftAsset(@PathVariable String category, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadDraftAsset(category, file));
    }

    @DeleteMapping("/draft/assets/{category}")
    public ResponseEntity<WebsiteEditorDto> deleteDraftAsset(@PathVariable String category) {
        return ResponseEntity.ok(service.deleteDraftAsset(category));
    }

    @PostMapping(value = "/draft/professionals/{draftKey}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebsiteEditorDto> uploadDraftProfessionalPhoto(@PathVariable String draftKey, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadDraftProfessionalPhoto(draftKey, file));
    }
}
