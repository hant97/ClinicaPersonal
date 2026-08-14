package com.clinica.backend.service;

import com.clinica.backend.dto.AuxiliaryExamDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.AuxiliaryExam;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AuxiliaryExamRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuxiliaryExamService {

    private final AuxiliaryExamRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<AuxiliaryExamDto> getExams(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<AuxiliaryExam> exams = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getId(), pageable);
        return exams.map(this::mapToDto);
    }

    @Transactional
    public AuxiliaryExamDto createExam(Long patientId, AuxiliaryExamDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        AuxiliaryExam exam = new AuxiliaryExam();
        exam.setPatient(patient);
        exam.setProfessionalId(user.getId());
        copyEditableFields(dto, exam);
        return mapToDto(repository.save(exam));
    }

    @Transactional
    public AuxiliaryExamDto updateExam(Long id, AuxiliaryExamDto dto) {
        AuxiliaryExam exam = getActiveExam(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", exam.getProfessionalId());
        copyEditableFields(dto, exam);
        return mapToDto(repository.save(exam));
    }

    @Transactional
    public void deleteExam(Long id) {
        AuxiliaryExam exam = getActiveExam(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", exam.getProfessionalId());
        exam.setDeleted(true);
        exam.setDeletedAt(LocalDateTime.now());
        exam.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(exam);
    }

    private AuxiliaryExam getActiveExam(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examen auxiliar no encontrado"));
    }

    private void copyEditableFields(AuxiliaryExamDto dto, AuxiliaryExam exam) {
        exam.setExamType(dto.getExamType());
        exam.setDescription(dto.getDescription());
        exam.setResult(dto.getResult());
        exam.setExamDate(dto.getExamDate());
        exam.setNotes(dto.getNotes());
    }

    private AuxiliaryExamDto mapToDto(AuxiliaryExam exam) {
        AuxiliaryExamDto dto = new AuxiliaryExamDto();
        dto.setId(exam.getId());
        dto.setPatientId(exam.getPatient().getId());
        dto.setExamType(exam.getExamType());
        dto.setDescription(exam.getDescription());
        dto.setResult(exam.getResult());
        dto.setExamDate(exam.getExamDate());
        dto.setNotes(exam.getNotes());
        dto.setProfessionalId(exam.getProfessionalId());
        dto.setCreatedAt(exam.getCreatedAt());
        dto.setUpdatedAt(exam.getUpdatedAt());
        return dto;
    }
}
