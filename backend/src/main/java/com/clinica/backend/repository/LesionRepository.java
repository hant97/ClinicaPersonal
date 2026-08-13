package com.clinica.backend.repository;

import com.clinica.backend.model.Lesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LesionRepository extends JpaRepository<Lesion, Long> {
    Page<Lesion> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Pageable pageable);
    Page<Lesion> findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Long professionalId, Pageable pageable);
    Optional<Lesion> findByIdAndDeletedFalse(Long id);
}
