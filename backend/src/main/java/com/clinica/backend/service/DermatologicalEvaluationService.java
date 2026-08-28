package com.clinica.backend.service;

import com.clinica.backend.mapper.DermatologicalEvaluationMapper;

import com.clinica.backend.dto.DermatologicalEvaluationDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.DermatologicalEvaluation;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.DermatologicalEvaluationRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DermatologicalEvaluationService {

    private final DermatologicalEvaluationRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final DermatologicalEvaluationMapper dermatologicalEvaluationMapper;

    @Transactional(readOnly = true)
    public Page<DermatologicalEvaluationDto> getEvaluationsByPatientId(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<DermatologicalEvaluation> evaluations = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalse(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalse(patientId, user.getId(), pageable);
        return evaluations.map(dermatologicalEvaluationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DermatologicalEvaluationDto getEvaluationById(Long id) {
        DermatologicalEvaluation evaluation = getActiveEvaluation(id);
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", evaluation.getProfessionalId());
        return dermatologicalEvaluationMapper.toDto(evaluation);
    }

    @Transactional
    public DermatologicalEvaluationDto createEvaluation(DermatologicalEvaluationDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        DermatologicalEvaluation evaluation = new DermatologicalEvaluation();
        evaluation.setPatient(patient);
        evaluation.setProfessionalId(user.getId());
        copyEditableFields(dto, evaluation);
        if (evaluation.getEvaluationDate() == null) {
            evaluation.setEvaluationDate(LocalDate.now());
        }
        return dermatologicalEvaluationMapper.toDto(repository.save(evaluation));
    }

    @Transactional
    public DermatologicalEvaluationDto updateEvaluation(Long id, DermatologicalEvaluationDto dto) {
        DermatologicalEvaluation evaluation = getActiveEvaluation(id);
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", evaluation.getProfessionalId());
        copyEditableFields(dto, evaluation);
        return dermatologicalEvaluationMapper.toDto(repository.save(evaluation));
    }

    @Transactional
    public void deleteEvaluation(Long id) {
        DermatologicalEvaluation evaluation = getActiveEvaluation(id);
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", evaluation.getProfessionalId());
        evaluation.setDeleted(true);
        evaluation.setDeletedAt(LocalDateTime.now());
        evaluation.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(evaluation);
    }

    private DermatologicalEvaluation getActiveEvaluation(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación dermatológica no encontrada"));
    }

    private void copyEditableFields(DermatologicalEvaluationDto dto, DermatologicalEvaluation evaluation) {
        if (dto.getEvaluationDate() != null) {
            evaluation.setEvaluationDate(dto.getEvaluationDate());
        }
        evaluation.setSkinType(dto.getSkinType());
        evaluation.setAffectedArea(dto.getAffectedArea());
        evaluation.setLesionType(dto.getLesionType());
        evaluation.setLesionSize(dto.getLesionSize());
        evaluation.setDermatologicalDiagnosis(dto.getDermatologicalDiagnosis());
        evaluation.setTreatmentIndicated(dto.getTreatmentIndicated());
        evaluation.setProcedurePerformed(dto.getProcedurePerformed());
        evaluation.setEvolutionNotes(dto.getEvolutionNotes());
        evaluation.setNextReviewDate(dto.getNextReviewDate());
    }

}
