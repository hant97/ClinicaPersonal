package com.clinica.backend.repository;

import com.clinica.backend.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByPatientIdAndDeletedFalseOrderByPaymentDateDesc(Long patientId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE p.deleted = false AND " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(p.patient.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.patient.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.paymentDate DESC")
    Page<Payment> findAllWithSearch(@org.springframework.data.repository.query.Param("searchTerm") String searchTerm, Pageable pageable);
}
