package com.clinica.backend.repository;

import com.clinica.backend.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByDeletedFalse();
}
