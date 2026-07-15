package com.clinica.backend.service;

import com.clinica.backend.dto.MedicalRecordDto;
import com.clinica.backend.model.MedicalRecord;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.MedicalRecordRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;

    public List<MedicalRecordDto> getRecordsByPatientId(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderBySessionDateDesc(patientId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MedicalRecordDto getRecordById(Long id) {
        MedicalRecord record = medicalRecordRepository.findById(id).orElseThrow();
        return mapToDto(record);
    }

    public MedicalRecordDto createRecord(MedicalRecordDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId()).orElseThrow();
        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setSessionDate(dto.getSessionDate());
        record.setReasonForConsultation(dto.getReasonForConsultation());
        record.setEvolutionNotes(dto.getEvolutionNotes());
        record.setPresumptiveDiagnosis(dto.getPresumptiveDiagnosis());
        record.setAgreementsAndTasks(dto.getAgreementsAndTasks());
        record.setDynamicData(dto.getDynamicData());

        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        return mapToDto(savedRecord);
    }

    public MedicalRecordDto updateRecord(Long id, MedicalRecordDto dto) {
        MedicalRecord record = medicalRecordRepository.findById(id).orElseThrow();
        
        record.setSessionDate(dto.getSessionDate());
        record.setReasonForConsultation(dto.getReasonForConsultation());
        record.setEvolutionNotes(dto.getEvolutionNotes());
        record.setPresumptiveDiagnosis(dto.getPresumptiveDiagnosis());
        record.setAgreementsAndTasks(dto.getAgreementsAndTasks());
        record.setDynamicData(dto.getDynamicData());

        MedicalRecord updatedRecord = medicalRecordRepository.save(record);
        return mapToDto(updatedRecord);
    }

    public void deleteRecord(Long id) {
        medicalRecordRepository.deleteById(id);
    }

    private MedicalRecordDto mapToDto(MedicalRecord record) {
        MedicalRecordDto dto = new MedicalRecordDto();
        dto.setId(record.getId());
        dto.setPatientId(record.getPatient().getId());
        dto.setSessionDate(record.getSessionDate());
        dto.setReasonForConsultation(record.getReasonForConsultation());
        dto.setEvolutionNotes(record.getEvolutionNotes());
        dto.setPresumptiveDiagnosis(record.getPresumptiveDiagnosis());
        dto.setAgreementsAndTasks(record.getAgreementsAndTasks());
        dto.setDynamicData(record.getDynamicData());
        return dto;
    }
}
