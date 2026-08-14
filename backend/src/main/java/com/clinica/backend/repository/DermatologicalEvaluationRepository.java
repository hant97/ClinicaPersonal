package com.clinica.backend.repository;

import com.clinica.backend.model.DermatologicalEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DermatologicalEvaluationRepository extends JpaRepository<DermatologicalEvaluation, Long> {
    Page<DermatologicalEvaluation> findByPatientIdAndDeletedFalse(Long patientId, Pageable pageable);
    Page<DermatologicalEvaluation> findByPatientIdAndProfessionalIdAndDeletedFalse(Long patientId, Long professionalId, Pageable pageable);
    Optional<DermatologicalEvaluation> findByIdAndDeletedFalse(Long id);

    long countByEvaluationDateBetween(LocalDate start, LocalDate end);
    long countByEvaluationDateBetweenAndProcedurePerformedIsNotNullAndProcedurePerformedNot(LocalDate start, LocalDate end, String notValue);
}
