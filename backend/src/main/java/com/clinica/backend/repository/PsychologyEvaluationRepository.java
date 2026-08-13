package com.clinica.backend.repository;

import com.clinica.backend.model.PsychologyEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PsychologyEvaluationRepository extends JpaRepository<PsychologyEvaluation, Long> {
    Page<PsychologyEvaluation> findByPatientIdAndDeletedFalse(Long patientId, Pageable pageable);
    Page<PsychologyEvaluation> findByPatientIdAndProfessionalIdAndDeletedFalse(Long patientId, Long professionalId, Pageable pageable);
    Optional<PsychologyEvaluation> findByIdAndDeletedFalse(Long id);
}
