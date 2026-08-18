package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
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
    private final ClinicalAuthorizationService clinicalAuthorizationService;

    @Transactional(readOnly = true)
    public Page<ClinicalSessionDto> getSessionsByPatientId(Long patientId, Pageable pageable) {
        User user = clinicalAuthorizationService.currentUser();
        patientRepository.findByIdAndSpecialtyAndDeletedFalse(patientId, user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        Page<ClinicalSession> sessions = clinicalAuthorizationService.isSpecialtyAdministrator(user, user.getSpecialty())
                ? sessionRepository.findByPatientIdAndSpecialtyAndDeletedFalseOrderBySessionDateDescStartTimeDesc(
                        patientId, user.getSpecialty(), pageable)
                : sessionRepository.findVisibleByPatientAndSpecialty(patientId, user.getSpecialty(), user.getId(), pageable);
        return sessions.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ClinicalSessionDto getSessionById(Long id) {
        ClinicalSession session = getActiveSession(id);
        clinicalAuthorizationService.ensureSameSpecialty(session.getSpecialty());
        if (session.isConfidential()) {
            clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(session.getSpecialty(), session.getProfessionalId());
        }
        return mapToDto(session);
    }

    @Transactional
    public ClinicalSessionDto createSession(ClinicalSessionDto dto) {
        User user = clinicalAuthorizationService.currentUser();
        Patient patient = patientRepository.findByIdAndSpecialtyAndDeletedFalse(dto.getPatientId(), user.getSpecialty())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

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

        return mapToDto(saved);
    }

    @Transactional
    public ClinicalSessionDto updateSession(Long id, ClinicalSessionDto dto) {
        ClinicalSession session = getActiveSession(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(session.getSpecialty(), session.getProfessionalId());
        copyEditableFields(dto, session);
        return mapToDto(sessionRepository.save(session));
    }

    @Transactional
    public void deleteSession(Long id) {
        ClinicalSession session = getActiveSession(id);
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator(session.getSpecialty(), session.getProfessionalId());
        session.setDeleted(true);
        session.setDeletedAt(LocalDateTime.now());
        session.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        sessionRepository.save(session);
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
    }

    private ClinicalSessionDto mapToDto(ClinicalSession entity) {
        ClinicalSessionDto dto = new ClinicalSessionDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatient().getId());
        dto.setSessionDate(entity.getSessionDate());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setSessionType(entity.getSessionType());
        dto.setModality(entity.getModality());
        dto.setStatus(entity.getStatus());
        dto.setSubjective(entity.getSubjective());
        dto.setObjective(entity.getObjective());
        dto.setAnalysis(entity.getAnalysis());
        dto.setPlan(entity.getPlan());
        dto.setConfidential(entity.isConfidential());
        dto.setSpecialty(entity.getSpecialty());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setProfessionalId(entity.getProfessionalId());
        dto.setAppointmentId(entity.getAppointmentId());
        return dto;
    }
}
