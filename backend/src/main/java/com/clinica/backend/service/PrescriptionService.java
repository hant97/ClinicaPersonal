package com.clinica.backend.service;

import com.clinica.backend.dto.PrescriptionDto;
import com.clinica.backend.dto.PrescriptionItemDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Prescription;
import com.clinica.backend.model.PrescriptionItem;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository repository;
    private final PatientRepository patientRepository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<PrescriptionDto> getPrescriptions(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        Page<Prescription> prescriptions = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? repository.findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), pageable)
                : repository.findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(patientId, user.getSpecialty(), user.getId(), pageable);
        return prescriptions.map(this::mapToDto);
    }

    @Transactional
    public PrescriptionDto createPrescription(Long patientId, PrescriptionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setSpecialty(user.getSpecialty());
        prescription.setProfessionalId(user.getId());
        copyEditableFields(dto, prescription);
        return mapToDto(repository.save(prescription));
    }

    @Transactional
    public PrescriptionDto updatePrescription(Long id, PrescriptionDto dto) {
        Prescription prescription = getActivePrescription(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(prescription.getSpecialty(), prescription.getProfessionalId());
        copyEditableFields(dto, prescription);
        return mapToDto(repository.save(prescription));
    }

    @Transactional
    public void deletePrescription(Long id) {
        Prescription prescription = getActivePrescription(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(prescription.getSpecialty(), prescription.getProfessionalId());
        prescription.setDeleted(true);
        prescription.setDeletedAt(LocalDateTime.now());
        prescription.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(prescription);
    }

    private Prescription getActivePrescription(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));
    }

    private void copyEditableFields(PrescriptionDto dto, Prescription prescription) {
        prescription.setPrescriptionDate(dto.getPrescriptionDate());
        prescription.setValidUntil(dto.getValidUntil());
        prescription.setNotes(dto.getNotes());
        replaceItems(dto, prescription);
    }

    private void replaceItems(PrescriptionDto dto, Prescription prescription) {
        if (prescription.getItems() == null) {
            prescription.setItems(new ArrayList<>());
        }
        prescription.getItems().clear();
        if (dto.getItems() != null) {
            for (PrescriptionItemDto itemDto : dto.getItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setPrescription(prescription);
                item.setName(itemDto.getName());
                item.setDose(itemDto.getDose());
                item.setFrequency(itemDto.getFrequency());
                item.setDuration(itemDto.getDuration());
                item.setRoute(itemDto.getRoute());
                item.setInstructions(itemDto.getInstructions());
                prescription.getItems().add(item);
            }
        }
    }

    private PrescriptionDto mapToDto(Prescription prescription) {
        PrescriptionDto dto = new PrescriptionDto();
        dto.setId(prescription.getId());
        dto.setPatientId(prescription.getPatient().getId());
        dto.setSpecialty(prescription.getSpecialty());
        dto.setPrescriptionDate(prescription.getPrescriptionDate());
        dto.setValidUntil(prescription.getValidUntil());
        dto.setNotes(prescription.getNotes());
        dto.setProfessionalId(prescription.getProfessionalId());
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setUpdatedAt(prescription.getUpdatedAt());
        dto.setItems(mapItems(prescription));
        return dto;
    }

    private List<PrescriptionItemDto> mapItems(Prescription prescription) {
        List<PrescriptionItemDto> items = new ArrayList<>();
        if (prescription.getItems() != null) {
            for (PrescriptionItem item : prescription.getItems()) {
                PrescriptionItemDto itemDto = new PrescriptionItemDto();
                itemDto.setId(item.getId());
                itemDto.setName(item.getName());
                itemDto.setDose(item.getDose());
                itemDto.setFrequency(item.getFrequency());
                itemDto.setDuration(item.getDuration());
                itemDto.setRoute(item.getRoute());
                itemDto.setInstructions(item.getInstructions());
                items.add(itemDto);
            }
        }
        return items;
    }
}
