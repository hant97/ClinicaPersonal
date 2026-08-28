package com.clinica.backend.service;

import com.clinica.backend.mapper.ClinicalSessionMapper;

import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.exception.BusinessRuleException;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClinicalSessionService {

    private final ClinicalSessionRepository sessionRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AuditLogService auditLogService;
    private final ClinicalSessionMapper clinicalSessionMapper;

    @Transactional(readOnly = true)
    public Page<ClinicalSessionDto> getSessionsByPatientId(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<ClinicalSession> sessions = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? sessionRepository.findByPatientIdAndSpecialtyAndDeletedFalseOrderBySessionDateDescStartTimeDesc(
                        patientId, user.getSpecialty(), pageable)
                : sessionRepository.findVisibleByPatientAndSpecialty(patientId, user.getSpecialty(), user.getId(), pageable);
        return sessions.map(clinicalSessionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ClinicalSessionDto getSessionById(Long id) {
        ClinicalSession session = getActiveSession(id);
        clinicalAuthorizationService.ensureSameSpecialty(session.getSpecialty());
        if (session.isConfidential()) {
            clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(session.getSpecialty(), session.getProfessionalId());
        }
        return withRiskAssessment(clinicalSessionMapper.toDto(session), session.getId());
    }

    @Transactional
    public ClinicalSessionDto createSession(ClinicalSessionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        ensureRiskAssessmentIfRequired(dto, patient.getId(), user.getSpecialty());

        ClinicalSession session = new ClinicalSession();
        session.setPatient(patient);
        copyEditableFields(dto, session);
        session.setSpecialty(user.getSpecialty());
        session.setProfessionalId(user.getId());
        ClinicalSession saved = sessionRepository.save(session);

        if (dto.getAppointmentId() != null) {
            appointmentRepository.findByIdAndSpecialty(dto.getAppointmentId(), user.getSpecialty())
                    .ifPresent(appointment -> {
                        appointment.setStatus("COMPLETADA");
                        appointment.setClinicalSessionId(saved.getId());
                        appointmentRepository.save(appointment);
                    });
        }

        if (hasMeaningfulRiskAssessment(dto, user.getSpecialty())) {
            riskAssessmentService.createOrUpdateForSession(saved.getId(), patient.getId(), dto.getRiskAssessment(), user);
        }

        auditLogService.record(
                "CREATE",
                "CLINICAL_SESSION",
                saved.getId().toString(),
                "Sesión clínica creada (Fecha: " + saved.getSessionDate() + ", Tipo: " + saved.getSessionType() + ") para paciente ID: " + patient.getId()
        );

        return withRiskAssessment(clinicalSessionMapper.toDto(saved), saved.getId());
    }

    @Transactional
    public ClinicalSessionDto updateSession(Long id, ClinicalSessionDto dto) {
        ClinicalSession session = getActiveSession(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(session.getSpecialty(), session.getProfessionalId());

        ensureRiskAssessmentIfRequired(dto, session.getPatient().getId(), session.getSpecialty());

        copyEditableFields(dto, session);
        ClinicalSession updated = sessionRepository.save(session);

        if (hasMeaningfulRiskAssessment(dto, session.getSpecialty())) {
            User user = clinicalAuthorizationService.currentUser();
            riskAssessmentService.createOrUpdateForSession(updated.getId(), updated.getPatient().getId(), dto.getRiskAssessment(), user);
        }

        auditLogService.record(
                "UPDATE",
                "CLINICAL_SESSION",
                updated.getId().toString(),
                "Sesión clínica actualizada ID: " + updated.getId()
        );

        return withRiskAssessment(clinicalSessionMapper.toDto(updated), updated.getId());
    }

    /**
     * Cuando el paciente tiene una alerta de riesgo activa y la sesión es de
     * Psicología, exige que se envíe una evaluación de riesgo completa
     * (nivel + ideación suicida informados) antes de permitir guardar la
     * atención. Es una defensa adicional a la validación del formulario.
     */
    private void ensureRiskAssessmentIfRequired(ClinicalSessionDto dto, Long patientId, String specialty) {
        if (!"PSICOLOGIA".equals(specialty)) {
            return;
        }
        boolean hasActiveAlert = riskAlertRepository.existsByPatientIdAndSpecialtyAndActiveTrue(patientId, specialty);
        if (!hasActiveAlert) {
            return;
        }
        boolean incomplete = dto.getRiskAssessment() == null
                || dto.getRiskAssessment().getSuicidalIdeation() == null
                || dto.getRiskAssessment().getRiskLevel() == null
                || dto.getRiskAssessment().getRiskLevel().isBlank();
        if (incomplete) {
            throw new BusinessRuleException(
                    "El paciente tiene una alerta de riesgo activa: debe completarse la evaluación de riesgo estructurada antes de guardar la atención");
        }
    }

    /**
     * Solo persiste la evaluación de riesgo si la sesión es de Psicología y
     * el bloque trae contenido real (nivel de riesgo informado); evita crear
     * filas vacías cuando el formulario simplemente incluye el grupo sin
     * completar (p. ej. en Dermatología, donde el bloque no aplica).
     */
    private boolean hasMeaningfulRiskAssessment(ClinicalSessionDto dto, String specialty) {
        return "PSICOLOGIA".equals(specialty)
                && dto.getRiskAssessment() != null
                && dto.getRiskAssessment().getRiskLevel() != null
                && !dto.getRiskAssessment().getRiskLevel().isBlank();
    }

    private ClinicalSessionDto withRiskAssessment(ClinicalSessionDto dto, Long sessionId) {
        riskAssessmentService.findByClinicalSessionId(sessionId).ifPresent(dto::setRiskAssessment);
        return dto;
    }

    @Transactional
    public void deleteSession(Long id) {
        ClinicalSession session = getActiveSession(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(session.getSpecialty(), session.getProfessionalId());
        session.setDeleted(true);
        session.setDeletedAt(LocalDateTime.now());
        session.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        sessionRepository.save(session);

        auditLogService.record(
                "DELETE",
                "CLINICAL_SESSION",
                id.toString(),
                "Sesión clínica eliminada lógicamente ID: " + id
        );
    }

    private ClinicalSession getActiveSession(Long id) {
        return sessionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión clínica no encontrada"));
    }

    private void copyEditableFields(ClinicalSessionDto dto, ClinicalSession session) {
        session.setSessionDate(dto.getSessionDate());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setSessionType(dto.getSessionType());
        session.setModality(dto.getModality());
        session.setStatus(dto.getStatus());
        session.setSubjective(dto.getSubjective());
        session.setObjective(dto.getObjective());
        session.setAnalysis(dto.getAnalysis());
        session.setPlan(dto.getPlan());
        session.setConfidential(dto.isConfidential());
        session.setAppointmentId(dto.getAppointmentId());
        session.setAttentionId(dto.getAttentionId());
    }

}
