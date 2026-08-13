package com.clinica.backend.repository;

import com.clinica.backend.model.Procedure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, Long> {
    Page<Procedure> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Pageable pageable);
    Page<Procedure> findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Long professionalId, Pageable pageable);
    Optional<Procedure> findByIdAndDeletedFalse(Long id);
}
