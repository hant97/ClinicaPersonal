package com.clinica.backend.repository;

import com.clinica.backend.model.ClinicalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicalSessionRepository extends JpaRepository<ClinicalSession, Long> {
    List<ClinicalSession> findByPatientIdOrderBySessionDateDescStartTimeDesc(Long patientId);
}
