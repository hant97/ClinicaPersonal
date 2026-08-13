package com.clinica.backend.repository;

import com.clinica.backend.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    Page<RiskAlert> findByPatientIdAndSpecialtyOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);

    Page<RiskAlert> findByPatientIdAndSpecialtyAndActiveTrueOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);

    boolean existsByPatientIdAndActiveTrue(Long patientId);

    boolean existsByPatientIdAndSpecialtyAndActiveTrue(Long patientId, String specialty);

    Page<RiskAlert> findBySpecialtyAndActiveTrueOrderByCreatedAtDesc(String specialty, Pageable pageable);
}
