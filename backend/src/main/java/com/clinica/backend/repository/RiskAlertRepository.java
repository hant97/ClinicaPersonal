package com.clinica.backend.repository;

import com.clinica.backend.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    List<RiskAlert> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<RiskAlert> findByPatientIdAndActiveTrueOrderByCreatedAtDesc(Long patientId);
    boolean existsByPatientIdAndActiveTrue(Long patientId);
    List<RiskAlert> findByActiveTrueOrderByCreatedAtDesc();
}
