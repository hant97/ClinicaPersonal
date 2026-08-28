package com.clinica.backend.service;

import com.clinica.backend.mapper.RiskAssessmentMapper;

import com.clinica.backend.dto.RiskAssessmentDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.RiskAssessment;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskAssessmentRepository repository;
    private final ClinicalAuthorizationService clinicalAuthorizationService;
    private final AuditLogService auditLogService;
    private final RiskAssessmentMapper riskAssessmentMapper;

    @Transactional(readOnly = true)
    public Page<RiskAssessmentDto> getHistoryByPatientId(Long patientId, Pageable pageable) {
        clinicalAuthorizationService.ensureSameSpecialty("PSICOLOGIA");
        return repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                .map(riskAssessmentMapper::toDto);
    }

    /**
     * Crea o actualiza (upsert) la evaluación de riesgo ligada 1:1 a una sesión clínica.
     * Se invoca dentro de la misma transacción que crea/actualiza la {@code ClinicalSession},
     * por lo que no valida especialidad/propiedad por separado: ese control ya lo aplicó
     * {@code ClinicalSessionService} sobre la sesión padre.
     */
    @Transactional
    public RiskAssessmentDto createOrUpdateForSession(Long clinicalSessionId, Long patientId, RiskAssessmentDto dto, User currentUser) {
        RiskAssessment assessment = repository.findByClinicalSessionIdAndDeletedFalse(clinicalSessionId)
                .orElseGet(RiskAssessment::new);
        assessment.setPatientId(patientId);
        assessment.setClinicalSessionId(clinicalSessionId);
        assessment.setProfessionalId(currentUser.getId());
        copyEditableFields(dto, assessment);
        RiskAssessment saved = repository.save(assessment);

        auditLogService.record(
                assessment.getId() == null ? "CREATE" : "UPDATE",
                "RISK_ASSESSMENT",
                saved.getId().toString(),
                "Evaluación de riesgo (nivel: " + saved.getRiskLevel() + ") para paciente ID: " + patientId
                        + " ligada a sesión clínica ID: " + clinicalSessionId
        );

        return riskAssessmentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<RiskAssessmentDto> findByClinicalSessionId(Long clinicalSessionId) {
        return repository.findByClinicalSessionIdAndDeletedFalse(clinicalSessionId)
                .map(riskAssessmentMapper::toDto);
    }

    @Transactional
    public void deleteAssessment(Long id) {
        RiskAssessment assessment = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación de riesgo no encontrada"));
        clinicalAuthorizationService.ensureOwnerOrSpecialtyAdministrator("PSICOLOGIA", assessment.getProfessionalId());
        assessment.setDeleted(true);
        assessment.setDeletedAt(LocalDateTime.now());
        assessment.setDeletedBy(clinicalAuthorizationService.currentUser().getId());
        repository.save(assessment);
    }

    private void copyEditableFields(RiskAssessmentDto dto, RiskAssessment assessment) {
        assessment.setSuicidalIdeation(dto.getSuicidalIdeation());
        assessment.setIdeationFrequency(dto.getIdeationFrequency());
        assessment.setHasPlan(dto.getHasPlan());
        assessment.setPlanDescription(dto.getPlanDescription());
        assessment.setMeansAccess(dto.getMeansAccess());
        assessment.setMeansDescription(dto.getMeansDescription());
        assessment.setProtectiveFactors(dto.getProtectiveFactors());
        assessment.setRiskLevel(dto.getRiskLevel());
        assessment.setActionTaken(dto.getActionTaken());
    }
}
