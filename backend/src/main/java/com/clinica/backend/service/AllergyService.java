package com.clinica.backend.service;

import com.clinica.backend.dto.AllergyDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AllergyMapper;
import com.clinica.backend.model.Allergy;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AllergyRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AllergyService {

    private final AllergyRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AllergyMapper allergyMapper;

    @Transactional(readOnly = true)
    public Page<AllergyDto> getAllergies(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<Allergy> allergies = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return allergies.map(allergyMapper::toDto);
    }

    @Transactional
    public AllergyDto createAllergy(Long patientId, AllergyDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Allergy allergy = new Allergy();
        allergy.setPatient(patient);
        allergy.setSpecialty(user.getSpecialty());
        allergy.setProfessionalId(user.getId());
        copyEditableFields(dto, allergy);
        return allergyMapper.toDto(repository.save(allergy));
    }

    @Transactional
    public AllergyDto updateAllergy(Long id, AllergyDto dto) {
        Allergy allergy = getActiveAllergy(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(allergy.getSpecialty(), allergy.getProfessionalId());
        copyEditableFields(dto, allergy);
        return allergyMapper.toDto(repository.save(allergy));
    }

    @Transactional
    public void deleteAllergy(Long id) {
        Allergy allergy = getActiveAllergy(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(allergy.getSpecialty(), allergy.getProfessionalId());
        allergy.setDeleted(true);
        allergy.setDeletedAt(LocalDateTime.now());
        allergy.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(allergy);
    }

    private Allergy getActiveAllergy(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alergia no encontrada"));
    }

    private void copyEditableFields(AllergyDto dto, Allergy allergy) {
        allergy.setAllergen(dto.getAllergen());
        allergy.setType(dto.getType());
        allergy.setSeverity(dto.getSeverity());
        allergy.setReaction(dto.getReaction());
        allergy.setActive(dto.isActive());
        allergy.setNotes(dto.getNotes());
    }
}
