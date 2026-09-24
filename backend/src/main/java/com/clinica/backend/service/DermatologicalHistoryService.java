package com.clinica.backend.service;

import com.clinica.backend.mapper.DermatologicalHistoryMapper;

import com.clinica.backend.dto.DermatologicalHistoryDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.DermatologicalHistory;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.DermatologicalHistoryRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DermatologicalHistoryService {

    private final DermatologicalHistoryRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final DermatologicalHistoryMapper dermatologicalHistoryMapper;

    @Transactional(readOnly = true)
    public DermatologicalHistoryDto getHistory(Long patientId) {
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        DermatologicalHistory history = repository.findByPatientIdAndDeletedFalse(patientId).orElse(null);
        if (history == null) {
            DermatologicalHistoryDto empty = new DermatologicalHistoryDto();
            empty.setPatientId(patientId);
            return empty;
        }
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", history.getProfessionalId());
        return dermatologicalHistoryMapper.toDto(history);
    }

    @Transactional
    public DermatologicalHistoryDto upsertHistory(Long patientId, DermatologicalHistoryDto dto) {
        User user = clinicalAuthorizationService.currentProfessional();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        DermatologicalHistory history = repository.findByPatientIdAndDeletedFalse(patientId)
                .orElse(null);

        if (history != null) {
            clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", history.getProfessionalId());
        } else {
            history = new DermatologicalHistory();
            history.setPatient(patient);
        }

        if (history.getProfessionalId() == null) {
            history.setProfessionalId(user.getId());
        }
        copyEditableFields(dto, history);
        return dermatologicalHistoryMapper.toDto(repository.save(history));
    }

    private void copyEditableFields(DermatologicalHistoryDto dto, DermatologicalHistory history) {
        history.setSkinType(dto.getSkinType());
        history.setSunExposureHabits(dto.getSunExposureHabits());
        history.setPersonalSkinHistory(dto.getPersonalSkinHistory());
        history.setFamilySkinHistory(dto.getFamilySkinHistory());
        history.setChronicConditions(dto.getChronicConditions());
        history.setExamFindings(dto.getExamFindings());
        history.setNotes(dto.getNotes());
    }

}
