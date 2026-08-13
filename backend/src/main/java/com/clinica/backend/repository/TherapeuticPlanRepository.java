package com.clinica.backend.repository;

import com.clinica.backend.model.TherapeuticPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TherapeuticPlanRepository extends JpaRepository<TherapeuticPlan, Long> {
    Page<TherapeuticPlan> findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);
    Page<TherapeuticPlan> findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Long professionalId, Pageable pageable);
    Optional<TherapeuticPlan> findByIdAndDeletedFalse(Long id);
}
