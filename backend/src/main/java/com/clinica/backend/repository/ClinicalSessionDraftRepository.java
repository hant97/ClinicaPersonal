package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalSessionDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClinicalSessionDraftRepository extends JpaRepository<ClinicalSessionDraft, Long> {

    Optional<ClinicalSessionDraft> findByProfessionalIdAndPatientId(Long professionalId, Long patientId);

    void deleteByProfessionalIdAndPatientId(Long professionalId, Long patientId);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
