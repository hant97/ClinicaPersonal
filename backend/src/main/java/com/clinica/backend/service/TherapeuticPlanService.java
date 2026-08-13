package com.clinica.backend.service;

import com.clinica.backend.dto.TherapeuticPlanDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.TherapeuticPlan;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.TherapeuticPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TherapeuticPlanService {

    private final TherapeuticPlanRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<TherapeuticPlanDto> getPlans(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<TherapeuticPlan> plans = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return plans.map(this::mapToDto);
    }

    @Transactional
    public TherapeuticPlanDto createPlan(Long patientId, TherapeuticPlanDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        TherapeuticPlan plan = new TherapeuticPlan();
        plan.setPatient(patient);
        plan.setSpecialty(user.getSpecialty());
        plan.setProfessionalId(user.getId());
        copyEditableFields(dto, plan);
        return mapToDto(repository.save(plan));
    }

    @Transactional
    public TherapeuticPlanDto updatePlan(Long id, TherapeuticPlanDto dto) {
        TherapeuticPlan plan = getActivePlan(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(plan.getSpecialty(), plan.getProfessionalId());
        copyEditableFields(dto, plan);
        return mapToDto(repository.save(plan));
    }

    @Transactional
    public void deletePlan(Long id) {
        TherapeuticPlan plan = getActivePlan(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(plan.getSpecialty(), plan.getProfessionalId());
        plan.setDeleted(true);
        plan.setDeletedAt(LocalDateTime.now());
        plan.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(plan);
    }

    private TherapeuticPlan getActivePlan(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan terapéutico no encontrado"));
    }

    private void copyEditableFields(TherapeuticPlanDto dto, TherapeuticPlan plan) {
        plan.setObjectives(dto.getObjectives());
        plan.setInterventions(dto.getInterventions());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setStatus(dto.getStatus() == null ? "ACTIVO" : dto.getStatus());
        plan.setNotes(dto.getNotes());
    }

    private TherapeuticPlanDto mapToDto(TherapeuticPlan plan) {
        TherapeuticPlanDto dto = new TherapeuticPlanDto();
        dto.setId(plan.getId());
        dto.setPatientId(plan.getPatient().getId());
        dto.setSpecialty(plan.getSpecialty());
        dto.setObjectives(plan.getObjectives());
        dto.setInterventions(plan.getInterventions());
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setStatus(plan.getStatus());
        dto.setNotes(plan.getNotes());
        dto.setProfessionalId(plan.getProfessionalId());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        return dto;
    }
}
