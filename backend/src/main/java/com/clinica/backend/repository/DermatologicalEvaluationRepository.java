package com.clinica.backend.repository;

import com.clinica.backend.model.DermatologicalEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DermatologicalEvaluationRepository extends JpaRepository<DermatologicalEvaluation, Long> {
    Page<DermatologicalEvaluation> findByPatientIdAndDeletedFalse(Long patientId, Pageable pageable);
    Page<DermatologicalEvaluation> findByPatientIdAndProfessionalIdAndDeletedFalse(Long patientId, Long professionalId, Pageable pageable);
    java.util.Optional<DermatologicalEvaluation> findByIdAndDeletedFalse(Long id);

    long countByEvaluationDateBetween(java.time.LocalDate start, java.time.LocalDate end);
    long countByEvaluationDateBetweenAndProcedurePerformedIsNotNullAndProcedurePerformedNot(java.time.LocalDate start, java.time.LocalDate end, String notValue);
}
