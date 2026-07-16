package com.clinica.backend.repository;

import com.clinica.backend.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    Page<RiskAlert> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<RiskAlert> findByPatientIdAndActiveTrueOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    boolean existsByPatientIdAndActiveTrue(Long patientId);

    Page<RiskAlert> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);
}
