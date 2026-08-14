package com.clinica.backend.service;

import com.clinica.backend.dto.GeneralHistoryDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.GeneralHistory;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.GeneralHistoryRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeneralHistoryService {

    private final GeneralHistoryRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public GeneralHistoryDto getGeneralHistory(Long patientId) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        GeneralHistory history = repository.findByPatientIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElse(null);
        if (history == null) {
            GeneralHistoryDto empty = new GeneralHistoryDto();
            empty.setPatientId(patientId);
            empty.setSpecialty(user.getSpecialty());
            return empty;
        }
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(history.getSpecialty(), history.getProfessionalId());
        return mapToDto(history);
    }

    @Transactional
    public GeneralHistoryDto upsertGeneralHistory(Long patientId, GeneralHistoryDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        GeneralHistory history = repository.findByPatientIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElse(null);

        if (history != null) {
            clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(history.getSpecialty(), history.getProfessionalId());
        } else {
            history = new GeneralHistory();
            history.setPatient(patient);
            history.setSpecialty(user.getSpecialty());
        }

        if (history.getProfessionalId() == null) {
            history.setProfessionalId(user.getId());
        }
        copyEditableFields(dto, history);
        return mapToDto(repository.save(history));
    }

    private void copyEditableFields(GeneralHistoryDto dto, GeneralHistory history) {
        history.setPathologicalHistory(dto.getPathologicalHistory());
        history.setSurgicalHistory(dto.getSurgicalHistory());
        history.setFamilyHistory(dto.getFamilyHistory());
        history.setHabits(dto.getHabits());
        history.setNotes(dto.getNotes());
    }

    private GeneralHistoryDto mapToDto(GeneralHistory history) {
        GeneralHistoryDto dto = new GeneralHistoryDto();
        dto.setId(history.getId());
        dto.setPatientId(history.getPatient().getId());
        dto.setSpecialty(history.getSpecialty());
        dto.setPathologicalHistory(history.getPathologicalHistory());
        dto.setSurgicalHistory(history.getSurgicalHistory());
        dto.setFamilyHistory(history.getFamilyHistory());
        dto.setHabits(history.getHabits());
        dto.setNotes(history.getNotes());
        dto.setProfessionalId(history.getProfessionalId());
        dto.setCreatedAt(history.getCreatedAt());
        dto.setUpdatedAt(history.getUpdatedAt());
        return dto;
    }
}
