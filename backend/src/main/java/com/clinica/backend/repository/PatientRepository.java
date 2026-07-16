package com.clinica.backend.repository;

import com.clinica.backend.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Page<Patient> findByDeletedFalse(Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deleted = false AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.identificationDocument LIKE CONCAT('%', :query, '%'))")
    Page<Patient> searchPatients(@Param("query") String query, Pageable pageable);
}
