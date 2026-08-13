package com.clinica.backend.repository;

import com.clinica.backend.model.Diagnosis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    Page<Diagnosis> findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);
    Page<Diagnosis> findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Long professionalId, Pageable pageable);
    Optional<Diagnosis> findByIdAndDeletedFalse(Long id);
}
