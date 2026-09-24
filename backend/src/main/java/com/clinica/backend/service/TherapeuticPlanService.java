package com.clinica.backend.service;

import com.clinica.backend.mapper.TherapeuticPlanMapper;

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
    private final TherapeuticPlanMapper therapeuticPlanMapper;

    @Transactional(readOnly = true)
    public Page<TherapeuticPlanDto> getPlans(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentProfessional();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<TherapeuticPlan> plans = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return plans.map(therapeuticPlanMapper::toDto);
    }

    @Transactional
    public TherapeuticPlanDto createPlan(Long patientId, TherapeuticPlanDto dto) {
        User user = clinicalAuthorizationService.currentProfessional();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        TherapeuticPlan plan = new TherapeuticPlan();
        plan.setPatient(patient);
        plan.setSpecialty(user.getSpecialty());
        plan.setProfessionalId(user.getId());
        copyEditableFields(dto, plan);
        return therapeuticPlanMapper.toDto(repository.save(plan));
    }

    @Transactional
    public TherapeuticPlanDto updatePlan(Long id, TherapeuticPlanDto dto) {
        TherapeuticPlan plan = getActivePlan(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(plan.getSpecialty(), plan.getProfessionalId());
        copyEditableFields(dto, plan);
        return therapeuticPlanMapper.toDto(repository.save(plan));
    }

    @Transactional
    public void deletePlan(Long id) {
        TherapeuticPlan plan = getActivePlan(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(plan.getSpecialty(), plan.getProfessionalId());
        plan.setDeleted(true);
        plan.setDeletedAt(LocalDateTime.now());
        plan.setDeletedBy(clinicalAuthorizationService.currentProfessional().getId());
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

}
