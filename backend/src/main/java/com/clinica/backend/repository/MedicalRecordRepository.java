package com.clinica.backend.repository;

import com.clinica.backend.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderBySessionDateDesc(Long patientId);
}
