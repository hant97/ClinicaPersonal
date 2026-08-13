package com.clinica.backend.repository;

import com.clinica.backend.model.Evolution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvolutionRepository extends JpaRepository<Evolution, Long> {
    Page<Evolution> findByPatientIdAndDeletedFalseOrderByControlDateDesc(Long patientId, Pageable pageable);
    Page<Evolution> findByPatientIdAndProfessionalIdAndDeletedFalseOrderByControlDateDesc(Long patientId, Long professionalId, Pageable pageable);
    Optional<Evolution> findByIdAndDeletedFalse(Long id);
}
