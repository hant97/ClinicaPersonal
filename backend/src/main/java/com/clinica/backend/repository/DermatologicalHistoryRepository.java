package com.clinica.backend.repository;

import com.clinica.backend.model.DermatologicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DermatologicalHistoryRepository extends JpaRepository<DermatologicalHistory, Long> {
    Optional<DermatologicalHistory> findByPatientIdAndDeletedFalse(Long patientId);
}
