package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, Long> {
    Page<ClinicalDocument> findByPatientIdAndSpecialtyAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Pageable pageable);
    Page<ClinicalDocument> findByPatientIdAndSpecialtyAndProfessionalIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId, String specialty, Long professionalId, Pageable pageable);
    Optional<ClinicalDocument> findByIdAndDeletedFalse(Long id);
}
