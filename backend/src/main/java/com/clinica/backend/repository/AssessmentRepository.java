package com.clinica.backend.repository;

import com.clinica.backend.model.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Page<Assessment> findByPatientIdOrderByAssessmentDateDesc(Long patientId, Pageable pageable);

    boolean existsByPsychometricTestId(Long testId);

    long countByAssessmentDateBetween(LocalDateTime start, LocalDateTime end);
}
