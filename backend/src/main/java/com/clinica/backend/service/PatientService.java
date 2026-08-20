package com.clinica.backend.service;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final WebsiteFileStorage fileStorage;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<PatientDto> getAllPatients(Boolean active, String gender, Pageable pageable) {
        String specialty = currentSpecialty();
        Page<Patient> patientsPage = patientRepository.findAllBySpecialty(specialty, active, gender, pageable);
        return mapPageToDto(patientsPage, specialty);
    }

    @Transactional(readOnly = true)
    public Page<PatientDto> searchPatients(String query, Boolean active, String gender, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPatients(active, gender, pageable);
        }
        String specialty = currentSpecialty();
        Page<Patient> patientsPage = patientRepository.searchPatients(query, specialty, active, gender, pageable);
        return mapPageToDto(patientsPage, specialty);
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

    @Transactional(readOnly = true)
    public PatientDto getPatientByIdOrUuid(String identifier) {
        String specialty = currentSpecialty();
        Patient patient = findPatientByIdOrUuid(identifier, specialty);
        boolean hasAlerts = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(patient.getId(), specialty);
        return mapToDto(patient, hasAlerts);
    }

    public Patient findPatientByIdOrUuid(String identifier, String specialty) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Paciente no encontrado");
        }
        try {
            UUID uuid = UUID.fromString(identifier.trim());
            return patientRepository.findByUuidAndSpecialtyAndDeletedFalse(uuid, specialty)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        } catch (IllegalArgumentException e) {
            try {
                Long id = Long.parseLong(identifier.trim());
                return patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
            } catch (NumberFormatException nfe) {
                throw new ResourceNotFoundException("Paciente no encontrado");
            }
        }
    }

    @Transactional(readOnly = true)
    public PatientDto getPatientById(Long id) {
        String specialty = currentSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        boolean hasAlerts = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(patient.getId(), specialty);
        return mapToDto(patient, hasAlerts);
    }

    @Transactional
    public PatientDto createPatient(PatientDto patientDto) {
        Patient patient = mapToEntity(patientDto);
        patient.setSpecialty(currentSpecialty());
        Patient savedPatient = patientRepository.save(patient);

        auditLogService.record(
                "CREATE",
                "PATIENT",
                savedPatient.getId().toString(),
                "Paciente creado: " + savedPatient.getFirstName() + " " + savedPatient.getLastName() +
                        (savedPatient.getIdentificationDocument() != null ? " (Doc: " + savedPatient.getIdentificationDocument() + ")" : "")
        );

        return mapToDto(savedPatient, false);
    }

    @Transactional
    public PatientDto updatePatient(Long id, PatientDto patientDto) {
        String specialty = currentSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

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
        boolean hasAlerts = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(updatedPatient.getId(), specialty);

        auditLogService.record(
                "UPDATE",
                "PATIENT",
                updatedPatient.getId().toString(),
                "Paciente actualizado: " + updatedPatient.getFirstName() + " " + updatedPatient.getLastName()
        );

        return mapToDto(updatedPatient, hasAlerts);
    }

    @Transactional
    public void deletePatient(Long id) {
        String specialty = currentSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        patient.setDeleted(true);
        patientRepository.save(patient);

        auditLogService.record(
                "DELETE",
                "PATIENT",
                id.toString(),
                "Paciente eliminado lógicamente: " + patient.getFirstName() + " " + patient.getLastName() + " (ID: " + id + ")"
        );
    }

    @Transactional
    public PatientDto uploadPhoto(Long id, MultipartFile file) {
        String specialty = currentSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        String previousKey = toAssetKey(patient.getPhotoUrl());
        String key = fileStorage.store(file, "patients", previousKey);
        patient.setPhotoUrl(fileStorage.publicUrl(key));
        patientRepository.save(patient);
        boolean hasAlerts = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(patient.getId(), specialty);

        auditLogService.record(
                "UPDATE",
                "PATIENT",
                id.toString(),
                "Foto de perfil actualizada para paciente: " + patient.getFirstName() + " " + patient.getLastName() + " (ID: " + id + ")"
        );

        return mapToDto(patient, hasAlerts);
    }

    private String currentSpecialty() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getSpecialty();
    }

    private Page<PatientDto> mapPageToDto(Page<Patient> patientsPage, String specialty) {
        List<Long> patientIds = patientsPage.getContent().stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

        Set<Long> alertPatientIds = patientIds.isEmpty()
                ? Set.of()
                : riskAlertRepository.findPatientIdsWithActiveAlertsByPatientIdsAndSpecialty(patientIds, specialty);

        return patientsPage.map(p -> mapToDto(p, alertPatientIds.contains(p.getId())));
    }

    private PatientDto mapToDto(Patient patient, boolean hasActiveAlerts) {
        PatientDto dto = new PatientDto();
        dto.setId(patient.getId());
        dto.setUuid(patient.getUuid());
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
        dto.setHasActiveAlerts(hasActiveAlerts);
        dto.setCreatedAt(patient.getCreatedAt());
        return dto;
    }

    private Patient mapToEntity(PatientDto dto) {
        Patient patient = new Patient();
        if (dto.getUuid() != null) {
            patient.setUuid(dto.getUuid());
        }
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
