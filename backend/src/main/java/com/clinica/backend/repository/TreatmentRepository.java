package com.clinica.backend.repository;

import com.clinica.backend.model.Treatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    Page<Treatment> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Pageable pageable);
    Page<Treatment> findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Long professionalId, Pageable pageable);
    Optional<Treatment> findByIdAndDeletedFalse(Long id);
}
