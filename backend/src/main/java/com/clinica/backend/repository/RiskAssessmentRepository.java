package com.clinica.backend.repository;

import com.clinica.backend.model.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    Page<RiskAssessment> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Pageable pageable);
    Optional<RiskAssessment> findByClinicalSessionIdAndDeletedFalse(Long clinicalSessionId);
    Optional<RiskAssessment> findByIdAndDeletedFalse(Long id);
}
