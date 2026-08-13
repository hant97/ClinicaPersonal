package com.clinica.backend.repository;

import com.clinica.backend.model.AuxiliaryExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuxiliaryExamRepository extends JpaRepository<AuxiliaryExam, Long> {
    Page<AuxiliaryExam> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Pageable pageable);
    Page<AuxiliaryExam> findByPatientIdAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, Long professionalId, Pageable pageable);
    Optional<AuxiliaryExam> findByIdAndDeletedFalse(Long id);
}
