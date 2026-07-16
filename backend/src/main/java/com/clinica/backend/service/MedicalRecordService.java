package com.clinica.backend.service;

import com.clinica.backend.dto.MedicalRecordDto;
import com.clinica.backend.model.MedicalRecord;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.MedicalRecordRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<MedicalRecordDto> getRecordsByPatientId(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicalRecordDto createRecord(MedicalRecordDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDiagnosis(dto.getDiagnosis());
        record.setCurrentMedication(dto.getCurrentMedication());
        record.setTreatmentPlan(dto.getTreatmentPlan());
        record.setTreatmentStatus(dto.getTreatmentStatus() == null ? "Activo" : dto.getTreatmentStatus());

        return mapToDto(medicalRecordRepository.save(record));
    }

    @Transactional
    public MedicalRecordDto updateRecord(Long id, MedicalRecordDto dto) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        record.setDiagnosis(dto.getDiagnosis());
        record.setCurrentMedication(dto.getCurrentMedication());
        record.setTreatmentPlan(dto.getTreatmentPlan());
        record.setTreatmentStatus(dto.getTreatmentStatus());

        return mapToDto(medicalRecordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long id) {
        medicalRecordRepository.deleteById(id);
    }

    private MedicalRecordDto mapToDto(MedicalRecord entity) {
        MedicalRecordDto dto = new MedicalRecordDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatient().getId());
        dto.setDiagnosis(entity.getDiagnosis());
        dto.setCurrentMedication(entity.getCurrentMedication());
        dto.setTreatmentPlan(entity.getTreatmentPlan());
        dto.setTreatmentStatus(entity.getTreatmentStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
