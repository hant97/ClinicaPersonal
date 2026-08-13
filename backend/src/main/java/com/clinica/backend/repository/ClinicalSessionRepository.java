package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ClinicalSessionRepository extends JpaRepository<ClinicalSession, Long> {
    Page<ClinicalSession> findByPatientIdAndSpecialtyAndDeletedFalseOrderBySessionDateDescStartTimeDesc(Long patientId, String specialty, Pageable pageable);
    @Query("""
            SELECT s FROM ClinicalSession s
            WHERE s.patient.id = :patientId AND s.specialty = :specialty AND s.deleted = false
              AND (s.isConfidential = false OR s.professionalId = :professionalId)
            ORDER BY s.sessionDate DESC, s.startTime DESC
            """)
    Page<ClinicalSession> findVisibleByPatientAndSpecialty(
            @Param("patientId") Long patientId,
            @Param("specialty") String specialty,
            @Param("professionalId") Long professionalId,
            Pageable pageable);
    java.util.Optional<ClinicalSession> findByIdAndDeletedFalse(Long id);
}
