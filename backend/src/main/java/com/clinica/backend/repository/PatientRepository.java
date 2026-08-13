package com.clinica.backend.repository;

import com.clinica.backend.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Page<Patient> findBySpecialtyAndDeletedFalse(String specialty, Pageable pageable);

    long countBySpecialtyAndDeletedFalse(String specialty);

    Optional<Patient> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.deleted = false AND p.specialty = :specialty AND p.createdAt >= :startDate AND p.createdAt < :endDate")
    long countNewPatientsBetween(@Param("specialty") String specialty, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    boolean existsByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    @Query("SELECT p FROM Patient p WHERE p.deleted = false AND p.specialty = :specialty AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.identificationDocument LIKE CONCAT('%', :query, '%'))")
    Page<Patient> searchPatients(@Param("query") String query, @Param("specialty") String specialty, Pageable pageable);
}
