package com.clinica.backend.service;

import com.clinica.backend.mapper.ProcedureMapper;

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
    private final ProcedureMapper procedureMapper;

    @Transactional(readOnly = true)
    public Page<ProcedureDto> getProcedures(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentProfessional();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<Procedure> procedures = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getId(), pageable);
        return procedures.map(procedureMapper::toDto);
    }

    @Transactional
    public ProcedureDto createProcedure(Long patientId, ProcedureDto dto) {
        User user = clinicalAuthorizationService.currentProfessional();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Procedure procedure = new Procedure();
        procedure.setPatient(patient);
        procedure.setProfessionalId(user.getId());
        copyEditableFields(dto, procedure);
        return procedureMapper.toDto(repository.save(procedure));
    }

    @Transactional
    public ProcedureDto updateProcedure(Long id, ProcedureDto dto) {
        Procedure procedure = getActiveProcedure(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", procedure.getProfessionalId());
        copyEditableFields(dto, procedure);
        return procedureMapper.toDto(repository.save(procedure));
    }

    @Transactional
    public void deleteProcedure(Long id) {
        Procedure procedure = getActiveProcedure(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", procedure.getProfessionalId());
        procedure.setDeleted(true);
        procedure.setDeletedAt(LocalDateTime.now());
        procedure.setDeletedBy(clinicalAuthorizationService.currentProfessional().getId());
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

}
