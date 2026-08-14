package com.clinica.backend.service;

import com.clinica.backend.dto.DiagnosisDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Diagnosis;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.DiagnosisRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<DiagnosisDto> getDiagnoses(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<Diagnosis> diagnoses = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return diagnoses.map(this::mapToDto);
    }

    @Transactional
    public DiagnosisDto createDiagnosis(Long patientId, DiagnosisDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setPatient(patient);
        diagnosis.setSpecialty(user.getSpecialty());
        diagnosis.setProfessionalId(user.getId());
        copyEditableFields(dto, diagnosis);
        return mapToDto(repository.save(diagnosis));
    }

    @Transactional
    public DiagnosisDto updateDiagnosis(Long id, DiagnosisDto dto) {
        Diagnosis diagnosis = getActiveDiagnosis(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(diagnosis.getSpecialty(), diagnosis.getProfessionalId());
        copyEditableFields(dto, diagnosis);
        return mapToDto(repository.save(diagnosis));
    }

    @Transactional
    public void deleteDiagnosis(Long id) {
        Diagnosis diagnosis = getActiveDiagnosis(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(diagnosis.getSpecialty(), diagnosis.getProfessionalId());
        diagnosis.setDeleted(true);
        diagnosis.setDeletedAt(LocalDateTime.now());
        diagnosis.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(diagnosis);
    }

    private Diagnosis getActiveDiagnosis(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnóstico no encontrado"));
    }

    private void copyEditableFields(DiagnosisDto dto, Diagnosis diagnosis) {
        diagnosis.setCategory(dto.getCategory());
        diagnosis.setDescription(dto.getDescription());
        diagnosis.setStatus(dto.getStatus() == null ? "ACTIVO" : dto.getStatus());
        diagnosis.setDiagnosisDate(dto.getDiagnosisDate());
        diagnosis.setNotes(dto.getNotes());
    }

    private DiagnosisDto mapToDto(Diagnosis diagnosis) {
        DiagnosisDto dto = new DiagnosisDto();
        dto.setId(diagnosis.getId());
        dto.setPatientId(diagnosis.getPatient().getId());
        dto.setSpecialty(diagnosis.getSpecialty());
        dto.setCategory(diagnosis.getCategory());
        dto.setDescription(diagnosis.getDescription());
        dto.setStatus(diagnosis.getStatus());
        dto.setDiagnosisDate(diagnosis.getDiagnosisDate());
        dto.setNotes(diagnosis.getNotes());
        dto.setProfessionalId(diagnosis.getProfessionalId());
        dto.setCreatedAt(diagnosis.getCreatedAt());
        dto.setUpdatedAt(diagnosis.getUpdatedAt());
        return dto;
    }
}
