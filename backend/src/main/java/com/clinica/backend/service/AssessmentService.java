package com.clinica.backend.service;

import com.clinica.backend.dto.AssessmentDto;
import com.clinica.backend.dto.PsychometricTestDto;
import com.clinica.backend.model.Assessment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PsychometricTestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private PsychometricTestRepository psychometricTestRepository;

    @Autowired
    private PatientRepository patientRepository;

    public Page<AssessmentDto> getAssessmentsByPatientId(Long patientId, Pageable pageable) {
        return assessmentRepository.findByPatientIdOrderByAssessmentDateDesc(patientId, pageable).map(this::mapToDto);
    }

    @Transactional
    public AssessmentDto saveAssessment(AssessmentDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        PsychometricTest test = psychometricTestRepository.findById(dto.getPsychometricTestId())
                .orElseThrow(() -> new RuntimeException("Test not found"));

        Assessment assessment = new Assessment();
        if (dto.getId() != null) {
            assessment = assessmentRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Assessment not found"));
        } else {
            assessment.setPatient(patient);
            assessment.setPsychometricTest(test);
        }

        assessment.setTotalScore(dto.getTotalScore());
        assessment.setAnswersJson(dto.getAnswersJson());
        assessment.setNotes(dto.getNotes());

        Assessment saved = assessmentRepository.save(assessment);
        return mapToDto(saved);
    }

    private AssessmentDto mapToDto(Assessment assessment) {
        AssessmentDto dto = new AssessmentDto();
        dto.setId(assessment.getId());
        dto.setPatientId(assessment.getPatient().getId());
        dto.setPsychometricTestId(assessment.getPsychometricTest().getId());
        dto.setTestName(assessment.getPsychometricTest().getName());
        dto.setAssessmentDate(assessment.getAssessmentDate());
        dto.setTotalScore(assessment.getTotalScore());
        dto.setAnswersJson(assessment.getAnswersJson());
        dto.setNotes(assessment.getNotes());
        return dto;
    }
}
