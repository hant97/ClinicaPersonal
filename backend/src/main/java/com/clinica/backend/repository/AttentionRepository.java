package com.clinica.backend.repository;

import com.clinica.backend.model.Attention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttentionRepository extends JpaRepository<Attention, Long> {

    @EntityGraph(attributePaths = {"patient", "professional", "appointment", "clinicalSession", "prescription", "payment", "clinicalService"})
    Optional<Attention> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"patient", "professional", "appointment", "clinicalSession", "prescription", "payment", "clinicalService"})
    Optional<Attention> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    Optional<Attention> findByAppointmentIdAndDeletedFalse(Long appointmentId);

    List<Attention> findByAttentionDateAndSpecialtyAndDeletedFalse(LocalDate date, String specialty);

    @EntityGraph(attributePaths = {"patient", "professional", "appointment", "clinicalSession", "prescription", "payment", "clinicalService"})
    @Query("SELECT a FROM Attention a " +
           "WHERE a.specialty = :specialty " +
           "AND a.deleted = false " +
           "AND (:patientId IS NULL OR a.patient.id = :patientId) " +
           "AND (:professionalId IS NULL OR a.professional.id = :professionalId) " +
           "AND (cast(:status as string) IS NULL OR a.status = :status) " +
           "AND (cast(:startDate as date) IS NULL OR a.attentionDate >= :startDate) " +
           "AND (cast(:endDate as date) IS NULL OR a.attentionDate <= :endDate) " +
           "AND (cast(:searchTerm as string) IS NULL OR " +
           "     LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%')) OR " +
           "     LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%')) OR " +
           "     LOWER(a.patient.identificationDocument) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%')) OR " +
           "     LOWER(a.motive) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%'))) " +
           "ORDER BY a.attentionDate DESC, a.startTime DESC NULLS LAST, a.id DESC")
    Page<Attention> searchAttentions(
            @Param("specialty") String specialty,
            @Param("patientId") Long patientId,
            @Param("professionalId") Long professionalId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    long countByAttentionDateAndSpecialtyAndDeletedFalse(LocalDate date, String specialty);

    long countByAttentionDateAndStatusAndSpecialtyAndDeletedFalse(LocalDate date, String status, String specialty);

    @EntityGraph(attributePaths = {"professional", "payment", "clinicalService"})
    List<Attention> findBySpecialtyAndAttentionDateBetweenAndDeletedFalse(
            String specialty, LocalDate startDate, LocalDate endDate);
}
