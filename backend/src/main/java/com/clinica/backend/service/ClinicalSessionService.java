package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ClinicalSessionService {

    private final ClinicalSessionRepository sessionRepository;
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public Page<ClinicalSessionDto> getSessionsByPatientId(Long patientId, Pageable pageable) {
        // TODO: Para la fase de confidencialidad, aquí se debería filtrar la lista
        // para asegurar que las sesiones confidenciales solo se devuelvan si el
        // usuario logueado es un profesional médico (ej. comprobando el rol del usuario
        // en SecurityContextHolder).
        return sessionRepository.findByPatientIdOrderBySessionDateDescStartTimeDesc(patientId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ClinicalSessionDto getSessionById(Long id) {
        ClinicalSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        // TODO: Validar permisos de lectura si session.isConfidential() es true.
        return mapToDto(session);
    }

    @Transactional
    public ClinicalSessionDto createSession(ClinicalSessionDto dto) {
        ClinicalSession session = mapToEntity(dto);
        ClinicalSession savedSession = sessionRepository.save(session);
        return mapToDto(savedSession);
    }

    @Transactional
    public ClinicalSessionDto updateSession(Long id, ClinicalSessionDto dto) {
        ClinicalSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // TODO: Validar permisos de edición si session.isConfidential() es true
        // o si el usuario actual no es el creador de la sesión.

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
        session.setProfessionalId(dto.getProfessionalId());
        session.setAppointmentId(dto.getAppointmentId());

        ClinicalSession updatedSession = sessionRepository.save(session);
        return mapToDto(updatedSession);
    }

    @Transactional
    public void deleteSession(Long id) {
        if (!sessionRepository.existsById(id)) {
            throw new RuntimeException("Session not found");
        }
        // Nota: A futuro podríamos implementar soft delete también aquí
        sessionRepository.deleteById(id);
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
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setProfessionalId(entity.getProfessionalId());
        dto.setAppointmentId(entity.getAppointmentId());
        return dto;
    }

    private ClinicalSession mapToEntity(ClinicalSessionDto dto) {
        ClinicalSession entity = new ClinicalSession();
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        entity.setPatient(patient);

        entity.setSessionDate(dto.getSessionDate());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setSessionType(dto.getSessionType());
        entity.setModality(dto.getModality());
        entity.setStatus(dto.getStatus());
        entity.setSubjective(dto.getSubjective());
        entity.setObjective(dto.getObjective());
        entity.setAnalysis(dto.getAnalysis());
        entity.setPlan(dto.getPlan());
        entity.setConfidential(dto.isConfidential());
        entity.setProfessionalId(dto.getProfessionalId());
        entity.setAppointmentId(dto.getAppointmentId());
        return entity;
    }
}
