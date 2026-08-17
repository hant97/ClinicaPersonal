package com.clinica.backend.service;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final WebsiteFileStorage fileStorage;

    public Page<PatientDto> getAllPatients(Boolean active, String gender, Pageable pageable) {
        return patientRepository.findAllBySpecialty(currentSpecialty(), active, gender, pageable)
                .map(this::mapToDto);
    }

    public Page<PatientDto> searchPatients(String query, Boolean active, String gender, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPatients(active, gender, pageable);
        }
        return patientRepository.searchPatients(query, currentSpecialty(), active, gender, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PatientStatsDto getStats() {
        String specialty = currentSpecialty();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime startOfNextMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        long totalPatients = patientRepository.countBySpecialtyAndDeletedFalse(specialty);
        long newThisMonth = patientRepository.countNewPatientsBetween(specialty, startOfMonth, startOfNextMonth);
        long withActiveAlerts = riskAlertRepository.countDistinctPatientsWithActiveAlerts(specialty);
        long minors = patientRepository.countMinorsBySpecialty(specialty, today.minusYears(18));

        return PatientStatsDto.builder()
                .totalPatients(totalPatients)
                .newThisMonth(newThisMonth)
                .withActiveAlerts(withActiveAlerts)
                .minors(minors)
                .build();
    }

    public PatientDto getPatientById(Long id) {
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, currentSpecialty()).orElseThrow();
        return mapToDto(patient);
    }

    public PatientDto createPatient(PatientDto patientDto) {
        Patient patient = mapToEntity(patientDto);
        patient.setSpecialty(currentSpecialty());
        Patient savedPatient = patientRepository.save(patient);
        return mapToDto(savedPatient);
    }

    public PatientDto updatePatient(Long id, PatientDto patientDto) {
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, currentSpecialty()).orElseThrow();

        patient.setFirstName(patientDto.getFirstName());
        patient.setLastName(patientDto.getLastName());
        patient.setIdentificationDocument(patientDto.getIdentificationDocument());
        patient.setDateOfBirth(patientDto.getDateOfBirth());
        patient.setContactNumber(patientDto.getContactNumber());
        patient.setEmail(patientDto.getEmail());
        patient.setOccupation(patientDto.getOccupation());
        patient.setMaritalStatus(patientDto.getMaritalStatus());
        patient.setEmergencyContact(patientDto.getEmergencyContact());
        patient.setReasonForConsultation(patientDto.getReasonForConsultation());
        patient.setGender(patientDto.getGender());
        patient.setAddress(patientDto.getAddress());
        patient.setGuardianName(patientDto.getGuardianName());
        patient.setGuardianContact(patientDto.getGuardianContact());
        patient.setHasLegalGuardian(patientDto.isHasLegalGuardian());
        patient.setPhotoUrl(trimToNull(patientDto.getPhotoUrl()));
        patient.setActive(patientDto.getActive() == null || patientDto.getActive());

        Patient updatedPatient = patientRepository.save(patient);
        return mapToDto(updatedPatient);
    }

    public void deletePatient(Long id) {
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, currentSpecialty()).orElseThrow();
        patient.setDeleted(true);
        patientRepository.save(patient);
    }

    @Transactional
    public PatientDto uploadPhoto(Long id, MultipartFile file) {
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, currentSpecialty()).orElseThrow();
        String previousKey = toAssetKey(patient.getPhotoUrl());
        String key = fileStorage.store(file, "patients", previousKey);
        patient.setPhotoUrl(fileStorage.publicUrl(key));
        patientRepository.save(patient);
        return mapToDto(patient);
    }

    private String currentSpecialty() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getSpecialty();
    }

    private PatientDto mapToDto(Patient patient) {
        PatientDto dto = new PatientDto();
        dto.setId(patient.getId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setIdentificationDocument(patient.getIdentificationDocument());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setContactNumber(patient.getContactNumber());
        dto.setEmail(patient.getEmail());
        dto.setOccupation(patient.getOccupation());
        dto.setMaritalStatus(patient.getMaritalStatus());
        dto.setEmergencyContact(patient.getEmergencyContact());
        dto.setReasonForConsultation(patient.getReasonForConsultation());
        dto.setGender(patient.getGender());
        dto.setAddress(patient.getAddress());
        dto.setGuardianName(patient.getGuardianName());
        dto.setGuardianContact(patient.getGuardianContact());
        dto.setHasLegalGuardian(patient.isHasLegalGuardian());
        dto.setPhotoUrl(patient.getPhotoUrl());
        dto.setActive(patient.isActive());
        dto.setSpecialty(patient.getSpecialty());
        dto.setDeleted(patient.isDeleted());
        dto.setHasActiveAlerts(riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(patient.getId(), patient.getSpecialty()));
        return dto;
    }

    private Patient mapToEntity(PatientDto dto) {
        Patient patient = new Patient();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setIdentificationDocument(dto.getIdentificationDocument());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setContactNumber(dto.getContactNumber());
        patient.setEmail(dto.getEmail());
        patient.setOccupation(dto.getOccupation());
        patient.setMaritalStatus(dto.getMaritalStatus());
        patient.setEmergencyContact(dto.getEmergencyContact());
        patient.setReasonForConsultation(dto.getReasonForConsultation());
        patient.setGender(dto.getGender());
        patient.setAddress(dto.getAddress());
        patient.setGuardianName(dto.getGuardianName());
        patient.setGuardianContact(dto.getGuardianContact());
        patient.setHasLegalGuardian(dto.isHasLegalGuardian());
        patient.setPhotoUrl(trimToNull(dto.getPhotoUrl()));
        patient.setActive(dto.getActive() == null || dto.getActive());
        patient.setDeleted(dto.isDeleted());
        return patient;
    }

    private String toAssetKey(String photoUrl) {
        String prefix = "/api/v1/public/website-assets/";
        if (photoUrl == null || !photoUrl.startsWith(prefix)) {
            return null;
        }
        return photoUrl.substring(prefix.length());
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
