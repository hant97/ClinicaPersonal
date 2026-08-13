package com.clinica.backend.service;

import com.clinica.backend.dto.EvolutionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Evolution;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.EvolutionRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EvolutionService {

    private final EvolutionRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<EvolutionDto> getEvolutions(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<Evolution> evolutions = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalseOrderByControlDateDesc(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalseOrderByControlDateDesc(patientId, user.getId(), pageable);
        return evolutions.map(this::mapToDto);
    }

    @Transactional
    public EvolutionDto createEvolution(Long patientId, EvolutionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Evolution evolution = new Evolution();
        evolution.setPatient(patient);
        evolution.setProfessionalId(user.getId());
        copyEditableFields(dto, evolution);
        if (evolution.getControlDate() == null) {
            evolution.setControlDate(LocalDate.now());
        }
        return mapToDto(repository.save(evolution));
    }

    @Transactional
    public EvolutionDto updateEvolution(Long id, EvolutionDto dto) {
        Evolution evolution = getActiveEvolution(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", evolution.getProfessionalId());
        copyEditableFields(dto, evolution);
        return mapToDto(repository.save(evolution));
    }

    @Transactional
    public void deleteEvolution(Long id) {
        Evolution evolution = getActiveEvolution(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", evolution.getProfessionalId());
        evolution.setDeleted(true);
        evolution.setDeletedAt(LocalDateTime.now());
        evolution.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(evolution);
    }

    private Evolution getActiveEvolution(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Control no encontrado"));
    }

    private void copyEditableFields(EvolutionDto dto, Evolution evolution) {
        if (dto.getControlDate() != null) {
            evolution.setControlDate(dto.getControlDate());
        }
        evolution.setClinicalNotes(dto.getClinicalNotes());
        evolution.setNextControlDate(dto.getNextControlDate());
    }

    private EvolutionDto mapToDto(Evolution evolution) {
        EvolutionDto dto = new EvolutionDto();
        dto.setId(evolution.getId());
        dto.setPatientId(evolution.getPatient().getId());
        dto.setControlDate(evolution.getControlDate());
        dto.setClinicalNotes(evolution.getClinicalNotes());
        dto.setNextControlDate(evolution.getNextControlDate());
        dto.setProfessionalId(evolution.getProfessionalId());
        dto.setCreatedAt(evolution.getCreatedAt());
        dto.setUpdatedAt(evolution.getUpdatedAt());
        return dto;
    }
}
