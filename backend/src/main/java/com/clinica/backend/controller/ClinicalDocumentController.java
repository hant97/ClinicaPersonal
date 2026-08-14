package com.clinica.backend.controller;

import com.clinica.backend.dto.ClinicalDocumentDto;
import com.clinica.backend.service.ClinicalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClinicalDocumentController {

    private final ClinicalDocumentService service;

    @GetMapping("/patients/{patientId}/clinical-documents")
    public ResponseEntity<Page<ClinicalDocumentDto>> getDocuments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getDocuments(patientId, PageRequest.of(page, size)));
    }

    @PostMapping(value = "/patients/{patientId}/clinical-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClinicalDocumentDto> uploadDocument(
            @PathVariable Long patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "documentDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate documentDate) {
        return ResponseEntity.ok(service.uploadDocument(patientId, file, category, name, documentDate));
    }

    @GetMapping("/clinical-documents/{id}/file")
    public ResponseEntity<Resource> getDocumentFile(@PathVariable Long id) {
        ClinicalDocumentDto metadata = service.getDocument(id);
        Resource resource = service.loadDocument(id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(metadata.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeFilename(metadata.getName()) + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/clinical-documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        service.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null) {
            return "documento";
        }
        return name.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
