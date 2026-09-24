package com.clinica.backend.service;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.PatientMapper;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.SearchText;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    private static final String PHOTO_CATEGORY = "patients";

    private final PatientRepository patientRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final ClinicalFileStorage fileStorage;
    private final AuditLogService auditLogService;
    private final PatientMapper patientMapper;

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
        Page<Patient> patientsPage = patientRepository.searchPatients(
                SearchText.normalize(query), specialty, active, gender, pageable);
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
                "Paciente creado (ID: " + savedPatient.getId() + ")"
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
        patient.setActive(patientDto.getActive() == null || patientDto.getActive());

        Patient updatedPatient = patientRepository.save(patient);
        boolean hasAlerts = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(updatedPatient.getId(), specialty);

        auditLogService.record(
                "UPDATE",
                "PATIENT",
                updatedPatient.getId().toString(),
                "Paciente actualizado (ID: " + updatedPatient.getId() + ")"
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
                "Paciente eliminado lógicamente (ID: " + id + ")"
        );
    }

    @Transactional
    public PatientDto uploadPhoto(Long id, MultipartFile file) {
        String specialty = currentSpecialty();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, specialty)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        String previousKey = patient.getPhotoKey();
        String key = fileStorage.store(file, PHOTO_CATEGORY);
        replaceFileAfterCompletion(key, previousKey);
        patient.setPhotoKey(key);
        patientRepository.save(patient);
        boolean hasAlerts = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(patient.getId(), specialty);

        auditLogService.record(
                "UPDATE",
                "PATIENT",
                id.toString(),
                "Foto de perfil actualizada (paciente ID: " + id + ")"
        );

        return mapToDto(patient, hasAlerts);
    }

    @Transactional(readOnly = true)
    public Resource loadPhoto(Long id) {
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(id, currentSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        if (patient.getPhotoKey() == null) {
            throw new ResourceNotFoundException("El paciente no tiene foto");
        }
        return fileStorage.load(patient.getPhotoKey());
    }

    /**
     * La foto anterior solo se borra si la base confirma el cambio; si hay rollback se borra la
     * nueva, para no dejar la ficha apuntando a un archivo inexistente ni archivos huérfanos.
     */
    private void replaceFileAfterCompletion(String newKey, String previousKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(previousKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                deleteQuietly(status == STATUS_COMMITTED ? previousKey : newKey);
            }
        });
    }

    private void deleteQuietly(String key) {
        try {
            fileStorage.delete(key);
        } catch (RuntimeException ex) {
            log.warn("No se pudo eliminar la foto de paciente {}: {}", key, ex.getMessage());
        }
    }

    private String photoUrl(Patient patient) {
        String key = patient.getPhotoKey();
        if (key == null || patient.getId() == null) return null;
        // El sufijo cambia con cada foto nueva y evita que el navegador muestre una versión en caché.
        String version = BlobStore.filenameOf(key);
        int dot = version.lastIndexOf('.');
        return "/api/v1/patients/" + patient.getId() + "/photo?v=" + (dot > 0 ? version.substring(0, dot) : version);
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
        PatientDto dto = patientMapper.toDto(patient);
        dto.setHasActiveAlerts(hasActiveAlerts);
        dto.setPhotoUrl(photoUrl(patient));
        return dto;
    }

    private Patient mapToEntity(PatientDto dto) {
        Patient patient = patientMapper.toEntity(dto);
        if (dto.getUuid() != null) {
            patient.setUuid(dto.getUuid());
        }
        patient.setActive(dto.getActive() == null || dto.getActive());
        return patient;
    }
}
