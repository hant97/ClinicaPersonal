package com.clinica.backend.service;

import com.clinica.backend.mapper.AssessmentMapper;

import com.clinica.backend.dto.AssessmentDto;
import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Assessment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PsychometricTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;

    private final PsychometricTestRepository psychometricTestRepository;

    private final PatientRepository patientRepository;

    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AssessmentMapper assessmentMapper;

    @Transactional(readOnly = true)
    public Page<AssessmentDto> getAssessmentsByPatientId(Long patientId, Pageable pageable) {
        String specialty = clinicalAuthorizationService.currentUser().getSpecialty();
        if (!patientRepository.existsByIdAndSpecialtyAndDeletedFalse(patientId, specialty)) {
            throw new ResourceNotFoundException("Paciente no encontrado");
        }
        return assessmentRepository.findByPatientIdOrderByAssessmentDateDesc(patientId, pageable).map(assessmentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AssessmentDto> getPatientEvolution(Long patientId) {
        String specialty = clinicalAuthorizationService.currentUser().getSpecialty();
        if (!patientRepository.existsByIdAndSpecialtyAndDeletedFalse(patientId, specialty)) {
            throw new ResourceNotFoundException("Paciente no encontrado");
        }
        return assessmentRepository.findByPatientIdOrderByAssessmentDateAsc(patientId).stream()
                .map(assessmentMapper::toDto)
                .toList();
    }

    @Transactional
    public AssessmentDto saveAssessment(AssessmentDto dto) {
        String specialty = clinicalAuthorizationService.currentUser().getSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        PsychometricTest test = psychometricTestRepository.findById(dto.getPsychometricTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Prueba psicométrica no encontrada"));

        Assessment assessment;
        if (dto.getId() != null) {
            assessment = assessmentRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));
            if (!assessment.getPatient().getId().equals(patient.getId())) {
                throw new AccessDeniedException("La evaluación no corresponde al paciente especificado");
            }
        } else {
            assessment = new Assessment();
            assessment.setPatient(patient);
            assessment.setPsychometricTest(test);
        }

        assessment.setTotalScore(dto.getTotalScore());
        assessment.setAnswersJson(dto.getAnswersJson());
        assessment.setNotes(dto.getNotes());

        Assessment saved = assessmentRepository.save(assessment);
        return assessmentMapper.toDto(saved);
    }

}
