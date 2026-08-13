package com.clinica.backend.repository;

import com.clinica.backend.model.GeneralHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeneralHistoryRepository extends JpaRepository<GeneralHistory, Long> {
    Optional<GeneralHistory> findByPatientIdAndSpecialtyAndDeletedFalse(Long patientId, String specialty);
}
