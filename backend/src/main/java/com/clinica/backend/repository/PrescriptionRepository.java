package com.clinica.backend.repository;

import com.clinica.backend.model.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Page<Prescription> findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);
    Page<Prescription> findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Long professionalId, Pageable pageable);
    Optional<Prescription> findByIdAndDeletedFalse(Long id);
}
