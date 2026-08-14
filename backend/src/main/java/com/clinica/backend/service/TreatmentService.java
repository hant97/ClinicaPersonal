package com.clinica.backend.service;

import com.clinica.backend.dto.TreatmentDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Treatment;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TreatmentService {

    private final TreatmentRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<TreatmentDto> getTreatments(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<Treatment> treatments = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getId(), pageable);
        return treatments.map(this::mapToDto);
    }

    @Transactional
    public TreatmentDto createTreatment(Long patientId, TreatmentDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Treatment treatment = new Treatment();
        treatment.setPatient(patient);
        treatment.setProfessionalId(user.getId());
        copyEditableFields(dto, treatment);
        return mapToDto(repository.save(treatment));
    }

    @Transactional
    public TreatmentDto updateTreatment(Long id, TreatmentDto dto) {
        Treatment treatment = getActiveTreatment(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", treatment.getProfessionalId());
        copyEditableFields(dto, treatment);
        return mapToDto(repository.save(treatment));
    }

    @Transactional
    public void deleteTreatment(Long id) {
        Treatment treatment = getActiveTreatment(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", treatment.getProfessionalId());
        treatment.setDeleted(true);
        treatment.setDeletedAt(LocalDateTime.now());
        treatment.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(treatment);
    }

    private Treatment getActiveTreatment(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tratamiento no encontrado"));
    }

    private void copyEditableFields(TreatmentDto dto, Treatment treatment) {
        treatment.setName(dto.getName());
        treatment.setDose(dto.getDose());
        treatment.setRoute(dto.getRoute());
        treatment.setFrequency(dto.getFrequency());
        treatment.setStartDate(dto.getStartDate());
        treatment.setEndDate(dto.getEndDate());
        treatment.setStatus(dto.getStatus() == null ? "ACTIVO" : dto.getStatus());
        treatment.setNotes(dto.getNotes());
    }

    private TreatmentDto mapToDto(Treatment treatment) {
        TreatmentDto dto = new TreatmentDto();
        dto.setId(treatment.getId());
        dto.setPatientId(treatment.getPatient().getId());
        dto.setName(treatment.getName());
        dto.setDose(treatment.getDose());
        dto.setRoute(treatment.getRoute());
        dto.setFrequency(treatment.getFrequency());
        dto.setStartDate(treatment.getStartDate());
        dto.setEndDate(treatment.getEndDate());
        dto.setStatus(treatment.getStatus());
        dto.setNotes(treatment.getNotes());
        dto.setProfessionalId(treatment.getProfessionalId());
        dto.setCreatedAt(treatment.getCreatedAt());
        dto.setUpdatedAt(treatment.getUpdatedAt());
        return dto;
    }
}
