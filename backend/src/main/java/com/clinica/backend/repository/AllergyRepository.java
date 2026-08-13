package com.clinica.backend.repository;

import com.clinica.backend.model.Allergy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    Page<Allergy> findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);
    Page<Allergy> findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Long professionalId, Pageable pageable);
    Optional<Allergy> findByIdAndDeletedFalse(Long id);
}
