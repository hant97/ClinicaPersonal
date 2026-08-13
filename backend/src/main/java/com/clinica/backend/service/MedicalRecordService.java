package com.clinica.backend.service;

import com.clinica.backend.dto.MedicalRecordDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.MedicalRecord;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.MedicalRecordRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<MedicalRecordDto> getRecordsByPatientId(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<MedicalRecord> records = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? medicalRecordRepository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : medicalRecordRepository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(
                        patientId, user.getSpecialty(), user.getId(), pageable);
        return records.map(this::mapToDto);
    }

    @Transactional
    public MedicalRecordDto createRecord(MedicalRecordDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setSpecialty(user.getSpecialty());
        record.setProfessionalId(user.getId());
        copyEditableFields(dto, record);
        return mapToDto(medicalRecordRepository.save(record));
    }

    @Transactional
    public MedicalRecordDto updateRecord(Long id, MedicalRecordDto dto) {
        MedicalRecord record = getActiveRecord(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(record.getSpecialty(), record.getProfessionalId());
        copyEditableFields(dto, record);
        return mapToDto(medicalRecordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long id) {
        MedicalRecord record = getActiveRecord(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(record.getSpecialty(), record.getProfessionalId());
        record.setDeleted(true);
        record.setDeletedAt(LocalDateTime.now());
        record.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        medicalRecordRepository.save(record);
    }

    private MedicalRecord getActiveRecord(Long id) {
        return medicalRecordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historia médica no encontrada"));
    }

    private void copyEditableFields(MedicalRecordDto dto, MedicalRecord record) {
        record.setDiagnosis(dto.getDiagnosis());
        record.setCurrentMedication(dto.getCurrentMedication());
        record.setTreatmentPlan(dto.getTreatmentPlan());
        record.setTreatmentStatus(dto.getTreatmentStatus() == null ? "Activo" : dto.getTreatmentStatus());
        record.setSkinType(dto.getSkinType());
        record.setKnownAllergies(dto.getKnownAllergies());
        record.setChronicConditions(dto.getChronicConditions());
        record.setSunExposureHabits(dto.getSunExposureHabits());
    }

    private MedicalRecordDto mapToDto(MedicalRecord entity) {
        MedicalRecordDto dto = new MedicalRecordDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatient().getId());
        dto.setDiagnosis(entity.getDiagnosis());
        dto.setCurrentMedication(entity.getCurrentMedication());
        dto.setTreatmentPlan(entity.getTreatmentPlan());
        dto.setTreatmentStatus(entity.getTreatmentStatus());
        dto.setSpecialty(entity.getSpecialty());
        dto.setSkinType(entity.getSkinType());
        dto.setKnownAllergies(entity.getKnownAllergies());
        dto.setChronicConditions(entity.getChronicConditions());
        dto.setSunExposureHabits(entity.getSunExposureHabits());
        dto.setProfessionalId(entity.getProfessionalId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
