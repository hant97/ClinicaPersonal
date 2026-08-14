package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalDocumentDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalDocument;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalDocumentRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClinicalDocumentService {

    private final ClinicalDocumentRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final ClinicalDocumentStorage fileStorage;

    @Transactional(readOnly = true)
    public Page<ClinicalDocumentDto> getDocuments(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<ClinicalDocument> documents = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return documents.map(this::mapToDto);
    }

    @Transactional
    public ClinicalDocumentDto uploadDocument(Long patientId, MultipartFile file, String category, String name, LocalDate documentDate) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        String key = fileStorage.store(file, "documents");
        ClinicalDocument document = new ClinicalDocument();
        document.setPatient(patient);
        document.setSpecialty(user.getSpecialty());
        document.setProfessionalId(user.getId());
        document.setCategory(normalizeCategory(category));
        document.setName(resolveName(name, file));
        document.setMimeType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setDocumentDate(documentDate);
        document.setFileUrl(key);
        return mapToDto(repository.save(document));
    }

    @Transactional(readOnly = true)
    public ClinicalDocumentDto getDocument(Long id) {
        ClinicalDocument document = getActiveDocument(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(document.getSpecialty(), document.getProfessionalId());
        return mapToDto(document);
    }

    @Transactional(readOnly = true)
    public Resource loadDocument(Long id) {
        ClinicalDocument document = getActiveDocument(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(document.getSpecialty(), document.getProfessionalId());
        return fileStorage.load(document.getFileUrl());
    }

    @Transactional
    public void deleteDocument(Long id) {
        ClinicalDocument document = getActiveDocument(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(document.getSpecialty(), document.getProfessionalId());
        document.setDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(document);
        fileStorage.delete(document.getFileUrl());
    }

    private ClinicalDocument getActiveDocument(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
    }

    private String normalizeCategory(String category) {
        return (category == null || category.isBlank()) ? "OTRO" : category.toUpperCase();
    }

    private String resolveName(String name, MultipartFile file) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "documento";
        }
        int dot = original.lastIndexOf('.');
        return dot > 0 ? original.substring(0, dot) : original;
    }

    private ClinicalDocumentDto mapToDto(ClinicalDocument document) {
        ClinicalDocumentDto dto = new ClinicalDocumentDto();
        dto.setId(document.getId());
        dto.setPatientId(document.getPatient().getId());
        dto.setSpecialty(document.getSpecialty());
        dto.setCategory(document.getCategory());
        dto.setName(document.getName());
        dto.setMimeType(document.getMimeType());
        dto.setSizeBytes(document.getSizeBytes());
        dto.setDocumentDate(document.getDocumentDate());
        dto.setProfessionalId(document.getProfessionalId());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }
}
