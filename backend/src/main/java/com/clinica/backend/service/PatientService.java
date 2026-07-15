package com.clinica.backend.service;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public List<PatientDto> getAllPatients() {
        return patientRepository.findByDeletedFalse().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PatientDto getPatientById(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        return mapToDto(patient);
    }

    public PatientDto createPatient(PatientDto patientDto) {
        Patient patient = mapToEntity(patientDto);
        Patient savedPatient = patientRepository.save(patient);
        return mapToDto(savedPatient);
    }

    public PatientDto updatePatient(Long id, PatientDto patientDto) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        
        patient.setFirstName(patientDto.getFirstName());
        patient.setLastName(patientDto.getLastName());
        patient.setIdentificationDocument(patientDto.getIdentificationDocument());
        patient.setDateOfBirth(patientDto.getDateOfBirth());
        patient.setContactNumber(patientDto.getContactNumber());
        patient.setEmail(patientDto.getEmail());
        patient.setOccupation(patientDto.getOccupation());
        patient.setMaritalStatus(patientDto.getMaritalStatus());
        patient.setEmergencyContact(patientDto.getEmergencyContact());
        patient.setReasonForConsultation(patientDto.getReasonForConsultation());
        patient.setGender(patientDto.getGender());
        patient.setAddress(patientDto.getAddress());
        patient.setGuardianName(patientDto.getGuardianName());
        patient.setGuardianContact(patientDto.getGuardianContact());

        Patient updatedPatient = patientRepository.save(patient);
        return mapToDto(updatedPatient);
    }

    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        patient.setDeleted(true);
        patientRepository.save(patient);
    }

    private PatientDto mapToDto(Patient patient) {
        PatientDto dto = new PatientDto();
        dto.setId(patient.getId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setIdentificationDocument(patient.getIdentificationDocument());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setContactNumber(patient.getContactNumber());
        dto.setEmail(patient.getEmail());
        dto.setOccupation(patient.getOccupation());
        dto.setMaritalStatus(patient.getMaritalStatus());
        dto.setEmergencyContact(patient.getEmergencyContact());
        dto.setReasonForConsultation(patient.getReasonForConsultation());
        dto.setGender(patient.getGender());
        dto.setAddress(patient.getAddress());
        dto.setGuardianName(patient.getGuardianName());
        dto.setGuardianContact(patient.getGuardianContact());
        dto.setDeleted(patient.isDeleted());
        return dto;
    }

    private Patient mapToEntity(PatientDto dto) {
        Patient patient = new Patient();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setIdentificationDocument(dto.getIdentificationDocument());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setContactNumber(dto.getContactNumber());
        patient.setEmail(dto.getEmail());
        patient.setOccupation(dto.getOccupation());
        patient.setMaritalStatus(dto.getMaritalStatus());
        patient.setEmergencyContact(dto.getEmergencyContact());
        patient.setReasonForConsultation(dto.getReasonForConsultation());
        patient.setGender(dto.getGender());
        patient.setAddress(dto.getAddress());
        patient.setGuardianName(dto.getGuardianName());
        patient.setGuardianContact(dto.getGuardianContact());
        patient.setDeleted(dto.isDeleted());
        return patient;
    }
}
