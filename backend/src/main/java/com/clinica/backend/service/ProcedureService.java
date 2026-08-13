package com.clinica.backend.service;

import com.clinica.backend.dto.ProcedureDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Procedure;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProcedureService {

    private final ProcedureRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<ProcedureDto> getProcedures(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<Procedure> procedures = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getId(), pageable);
        return procedures.map(this::mapToDto);
    }

    @Transactional
    public ProcedureDto createProcedure(Long patientId, ProcedureDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Procedure procedure = new Procedure();
        procedure.setPatient(patient);
        procedure.setProfessionalId(user.getId());
        copyEditableFields(dto, procedure);
        return mapToDto(repository.save(procedure));
    }

    @Transactional
    public ProcedureDto updateProcedure(Long id, ProcedureDto dto) {
        Procedure procedure = getActiveProcedure(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", procedure.getProfessionalId());
        copyEditableFields(dto, procedure);
        return mapToDto(repository.save(procedure));
    }

    @Transactional
    public void deleteProcedure(Long id) {
        Procedure procedure = getActiveProcedure(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", procedure.getProfessionalId());
        procedure.setDeleted(true);
        procedure.setDeletedAt(LocalDateTime.now());
        procedure.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(procedure);
    }

    private Procedure getActiveProcedure(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento no encontrado"));
    }

    private void copyEditableFields(ProcedureDto dto, Procedure procedure) {
        procedure.setName(dto.getName());
        procedure.setDescription(dto.getDescription());
        procedure.setProcedureDate(dto.getProcedureDate());
        procedure.setNotes(dto.getNotes());
    }

    private ProcedureDto mapToDto(Procedure procedure) {
        ProcedureDto dto = new ProcedureDto();
        dto.setId(procedure.getId());
        dto.setPatientId(procedure.getPatient().getId());
        dto.setName(procedure.getName());
        dto.setDescription(procedure.getDescription());
        dto.setProcedureDate(procedure.getProcedureDate());
        dto.setNotes(procedure.getNotes());
        dto.setProfessionalId(procedure.getProfessionalId());
        dto.setCreatedAt(procedure.getCreatedAt());
        dto.setUpdatedAt(procedure.getUpdatedAt());
        return dto;
    }
}
