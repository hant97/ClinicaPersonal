package com.clinica.backend.repository;

import com.clinica.backend.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByPatientIdAndSpecialtyAndDeletedFalseOrderByPaymentDateDesc(Long patientId, String specialty, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND p.paymentDate >= :startDate AND p.paymentDate < :endDate")
    BigDecimal sumIncomeBetweenBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty);

    @Query("SELECT p FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(p.patient.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.patient.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.paymentDate DESC")
    Page<Payment> findAllWithSearchBySpecialty(@Param("searchTerm") String searchTerm, @Param("specialty") String specialty, Pageable pageable);
}
