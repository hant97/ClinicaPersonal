package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ClinicalSessionRepository extends JpaRepository<ClinicalSession, Long> {
    Page<ClinicalSession> findByPatientIdOrderBySessionDateDescStartTimeDesc(Long patientId, Pageable pageable);
}
