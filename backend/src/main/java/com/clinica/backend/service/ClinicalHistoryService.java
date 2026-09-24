package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalHistoryDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.service.provider.ClinicalHistorySectionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalHistoryService {

    private static final int AGGREGATE_PAGE_SIZE = 1000;

    private final PatientRepository patientRepository;
    private final GeneralHistoryService generalHistoryService;
    private final AllergyService allergyService;
    private final MedicationService medicationService;
    private final DiagnosisService diagnosisService;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final List<ClinicalHistorySectionProvider> sectionProviders;

    @Transactional(readOnly = true)
    public ClinicalHistoryDto getClinicalHistory(Long patientId) {
        User user = clinicalAuthorizationService.currentProfessional();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Pageable pageable = PageRequest.of(0, AGGREGATE_PAGE_SIZE);

        ClinicalHistoryDto dto = new ClinicalHistoryDto();
        dto.setPatientId(patientId);
        dto.setSpecialty(user.getSpecialty());

        // Secciones clínicas transversales comunes
        dto.setGeneralHistory(generalHistoryService.getGeneralHistory(patientId));
        dto.setAllergies(allergyService.getAllergies(patientId, pageable).getContent());
        dto.setMedications(medicationService.getMedications(patientId, pageable).getContent());
        dto.setDiagnoses(diagnosisService.getDiagnoses(patientId, pageable).getContent());

        // Delegación dinámica al provider de la especialidad activa
        sectionProviders.stream()
                .filter(provider -> provider.getSupportedSpecialty().equalsIgnoreCase(user.getSpecialty()))
                .findFirst()
                .ifPresent(provider -> provider.populateSections(patientId, dto, pageable));

        return dto;
    }
}
