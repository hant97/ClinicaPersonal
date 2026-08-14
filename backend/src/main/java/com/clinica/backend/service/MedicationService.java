package com.clinica.backend.service;

import com.clinica.backend.dto.MedicationDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Medication;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.MedicationRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<MedicationDto> getMedications(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<Medication> medications = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return medications.map(this::mapToDto);
    }

    @Transactional
    public MedicationDto createMedication(Long patientId, MedicationDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Medication medication = new Medication();
        medication.setPatient(patient);
        medication.setSpecialty(user.getSpecialty());
        medication.setProfessionalId(user.getId());
        copyEditableFields(dto, medication);
        return mapToDto(repository.save(medication));
    }

    @Transactional
    public MedicationDto updateMedication(Long id, MedicationDto dto) {
        Medication medication = getActiveMedication(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(medication.getSpecialty(), medication.getProfessionalId());
        copyEditableFields(dto, medication);
        return mapToDto(repository.save(medication));
    }

    @Transactional
    public void deleteMedication(Long id) {
        Medication medication = getActiveMedication(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(medication.getSpecialty(), medication.getProfessionalId());
        medication.setDeleted(true);
        medication.setDeletedAt(LocalDateTime.now());
        medication.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(medication);
    }

    private Medication getActiveMedication(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado"));
    }

    private void copyEditableFields(MedicationDto dto, Medication medication) {
        medication.setName(dto.getName());
        medication.setDose(dto.getDose());
        medication.setFrequency(dto.getFrequency());
        medication.setStartDate(dto.getStartDate());
        medication.setEndDate(dto.getEndDate());
        medication.setActive(dto.isActive());
        medication.setNotes(dto.getNotes());
    }

    private MedicationDto mapToDto(Medication medication) {
        MedicationDto dto = new MedicationDto();
        dto.setId(medication.getId());
        dto.setPatientId(medication.getPatient().getId());
        dto.setSpecialty(medication.getSpecialty());
        dto.setName(medication.getName());
        dto.setDose(medication.getDose());
        dto.setFrequency(medication.getFrequency());
        dto.setStartDate(medication.getStartDate());
        dto.setEndDate(medication.getEndDate());
        dto.setActive(medication.isActive());
        dto.setNotes(medication.getNotes());
        dto.setProfessionalId(medication.getProfessionalId());
        dto.setCreatedAt(medication.getCreatedAt());
        dto.setUpdatedAt(medication.getUpdatedAt());
        return dto;
    }
}
