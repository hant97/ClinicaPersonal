package com.clinica.backend.repository;

import com.clinica.backend.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    Page<RiskAlert> findByPatientIdAndSpecialtyOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);

    Page<RiskAlert> findByPatientIdAndSpecialtyAndActiveTrueOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);

    boolean existsByPatientIdAndActiveTrue(Long patientId);

    boolean existsByPatientIdAndSpecialtyAndActiveTrue(Long patientId, String specialty);

    Page<RiskAlert> findBySpecialtyAndActiveTrueOrderByCreatedAtDesc(String specialty, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT r.patientId) FROM RiskAlert r WHERE r.specialty = :specialty AND r.active = true")
    long countDistinctPatientsWithActiveAlerts(@Param("specialty") String specialty);

    @Query("SELECT DISTINCT r.patientId FROM RiskAlert r WHERE r.specialty = :specialty AND r.active = true AND r.patientId IN :patientIds")
    Set<Long> findPatientIdsWithActiveAlertsByPatientIdsAndSpecialty(@Param("patientIds") Collection<Long> patientIds, @Param("specialty") String specialty);
}
