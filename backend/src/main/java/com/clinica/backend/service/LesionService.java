package com.clinica.backend.service;

import com.clinica.backend.dto.LesionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Lesion;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.LesionRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LesionService {

    private final LesionRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<LesionDto> getLesions(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<Lesion> lesions = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                : repository.findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getId(), pageable);
        return lesions.map(this::mapToDto);
    }

    @Transactional
    public LesionDto createLesion(Long patientId, LesionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        clinicalAuthorizationService.ensureSameSpecialty("DERMATOLOGIA");
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, "DERMATOLOGIA")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Lesion lesion = new Lesion();
        lesion.setPatient(patient);
        lesion.setProfessionalId(user.getId());
        copyEditableFields(dto, lesion);
        return mapToDto(repository.save(lesion));
    }

    @Transactional
    public LesionDto updateLesion(Long id, LesionDto dto) {
        Lesion lesion = getActiveLesion(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", lesion.getProfessionalId());
        copyEditableFields(dto, lesion);
        return mapToDto(repository.save(lesion));
    }

    @Transactional
    public void deleteLesion(Long id) {
        Lesion lesion = getActiveLesion(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("DERMATOLOGIA", lesion.getProfessionalId());
        lesion.setDeleted(true);
        lesion.setDeletedAt(LocalDateTime.now());
        lesion.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(lesion);
    }

    private Lesion getActiveLesion(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesión no encontrada"));
    }

    private void copyEditableFields(LesionDto dto, Lesion lesion) {
        lesion.setBodyArea(dto.getBodyArea());
        lesion.setLesionType(dto.getLesionType());
        lesion.setSize(dto.getSize());
        lesion.setMorphology(dto.getMorphology());
        lesion.setColor(dto.getColor());
        lesion.setSinceDate(dto.getSinceDate());
        lesion.setEvolution(dto.getEvolution());
        lesion.setNotes(dto.getNotes());
    }

    private LesionDto mapToDto(Lesion lesion) {
        LesionDto dto = new LesionDto();
        dto.setId(lesion.getId());
        dto.setPatientId(lesion.getPatient().getId());
        dto.setBodyArea(lesion.getBodyArea());
        dto.setLesionType(lesion.getLesionType());
        dto.setSize(lesion.getSize());
        dto.setMorphology(lesion.getMorphology());
        dto.setColor(lesion.getColor());
        dto.setSinceDate(lesion.getSinceDate());
        dto.setEvolution(lesion.getEvolution());
        dto.setNotes(lesion.getNotes());
        dto.setProfessionalId(lesion.getProfessionalId());
        dto.setCreatedAt(lesion.getCreatedAt());
        dto.setUpdatedAt(lesion.getUpdatedAt());
        return dto;
    }
}
