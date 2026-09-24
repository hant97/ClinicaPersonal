package com.clinica.backend.repository;

import com.clinica.backend.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Page<Patient> findBySpecialtyAndDeletedFalse(String specialty, Pageable pageable);

    long countBySpecialtyAndDeletedFalse(String specialty);

    Optional<Patient> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    Optional<Patient> findByUuidAndSpecialtyAndDeletedFalse(UUID uuid, String specialty);

    Optional<Patient> findByUuidAndDeletedFalse(UUID uuid);

    boolean existsByUuidAndSpecialtyAndDeletedFalse(UUID uuid, String specialty);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.deleted = false AND p.specialty = :specialty AND p.createdAt >= :startDate AND p.createdAt < :endDate")
    long countNewPatientsBetween(@Param("specialty") String specialty, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.deleted = false AND p.specialty = :specialty AND p.dateOfBirth > :cutoff")
    long countMinorsBySpecialty(@Param("specialty") String specialty, @Param("cutoff") LocalDate cutoff);

    boolean existsByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    @Query("SELECT p FROM Patient p WHERE p.deleted = false AND p.specialty = :specialty AND " +
           "(:active IS NULL OR p.active = :active) AND " +
           "(cast(:gender as string) IS NULL OR p.gender = :gender) " +
           "ORDER BY p.lastName ASC, p.firstName ASC")
    Page<Patient> findAllBySpecialty(@Param("specialty") String specialty, @Param("active") Boolean active, @Param("gender") String gender, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deleted = false AND p.specialty = :specialty AND " +
           "(:active IS NULL OR p.active = :active) AND " +
           "(cast(:gender as string) IS NULL OR p.gender = :gender) AND " +
           "p.searchText LIKE CONCAT('%', cast(:normalizedQuery as string), '%')")
    // normalizedQuery: SearchText.normalize(query), sin tildes ni mayúsculas, igual que p.searchText.
    Page<Patient> searchPatients(@Param("normalizedQuery") String normalizedQuery, @Param("specialty") String specialty, @Param("active") Boolean active, @Param("gender") String gender, Pageable pageable);
}
