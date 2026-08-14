package com.clinica.backend.service;

import com.clinica.backend.dto.PsychologyEvaluationDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.PsychologyEvaluation;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PsychologyEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PsychologyEvaluationService {

    private final PsychologyEvaluationRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<PsychologyEvaluationDto> getEvaluationsByPatientId(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<PsychologyEvaluation> evaluations = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalse(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalse(patientId, user.getId(), pageable);
        return evaluations.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PsychologyEvaluationDto getEvaluationById(Long id) {
        PsychologyEvaluation evaluation = getActiveEvaluation(id);
        clinicalAuthorizationService.ensureSameSpecialty("PSICOLOGIA");
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", evaluation.getProfessionalId());
        return mapToDto(evaluation);
    }

    @Transactional
    public PsychologyEvaluationDto createEvaluation(PsychologyEvaluationDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("PSICOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        PsychologyEvaluation evaluation = new PsychologyEvaluation();
        evaluation.setPatient(patient);
        evaluation.setProfessionalId(user.getId());
        copyEditableFields(dto, evaluation);
        if (evaluation.getEvaluationDate() == null) {
            evaluation.setEvaluationDate(LocalDate.now());
        }
        return mapToDto(repository.save(evaluation));
    }

    @Transactional
    public PsychologyEvaluationDto updateEvaluation(Long id, PsychologyEvaluationDto dto) {
        PsychologyEvaluation evaluation = getActiveEvaluation(id);
        clinicalAuthorizationService.ensureSameSpecialty("PSICOLOGIA");
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", evaluation.getProfessionalId());
        copyEditableFields(dto, evaluation);
        return mapToDto(repository.save(evaluation));
    }

    @Transactional
    public void deleteEvaluation(Long id) {
        PsychologyEvaluation evaluation = getActiveEvaluation(id);
        clinicalAuthorizationService.ensureSameSpecialty("PSICOLOGIA");
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", evaluation.getProfessionalId());
        evaluation.setDeleted(true);
        evaluation.setDeletedAt(LocalDateTime.now());
        evaluation.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(evaluation);
    }

    private PsychologyEvaluation getActiveEvaluation(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación psicológica no encontrada"));
    }

    private void copyEditableFields(PsychologyEvaluationDto dto, PsychologyEvaluation evaluation) {
        if (dto.getEvaluationDate() != null) {
            evaluation.setEvaluationDate(dto.getEvaluationDate());
        }
        evaluation.setInitialEvaluation(dto.getInitialEvaluation());
        evaluation.setPsychologicalHistory(dto.getPsychologicalHistory());
        evaluation.setMentalExam(dto.getMentalExam());
        evaluation.setNotes(dto.getNotes());
    }

    private PsychologyEvaluationDto mapToDto(PsychologyEvaluation evaluation) {
        PsychologyEvaluationDto dto = new PsychologyEvaluationDto();
        dto.setId(evaluation.getId());
        dto.setPatientId(evaluation.getPatient().getId());
        dto.setEvaluationDate(evaluation.getEvaluationDate());
        dto.setInitialEvaluation(evaluation.getInitialEvaluation());
        dto.setPsychologicalHistory(evaluation.getPsychologicalHistory());
        dto.setMentalExam(evaluation.getMentalExam());
        dto.setNotes(evaluation.getNotes());
        dto.setProfessionalId(evaluation.getProfessionalId());
        dto.setCreatedAt(evaluation.getCreatedAt());
        dto.setUpdatedAt(evaluation.getUpdatedAt());
        return dto;
    }
}
