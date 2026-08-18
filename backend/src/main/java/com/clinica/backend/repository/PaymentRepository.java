package com.clinica.backend.repository;

import com.clinica.backend.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByPatientIdAndSpecialtyAndDeletedFalseOrderByPaymentDateDesc(Long patientId, String specialty, Pageable pageable);

    Optional<Payment> findFirstByAppointmentIdAndDeletedFalse(Long appointmentId);

    List<Payment> findByAppointmentIdInAndDeletedFalse(Collection<Long> appointmentIds);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND p.paymentDate >= :startDate AND p.paymentDate < :endDate")
    BigDecimal sumIncomeBetweenBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty);

    @Query("SELECT p FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(p.patient.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.patient.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:paymentMethod IS NULL OR :paymentMethod = '' OR p.paymentMethod = :paymentMethod) AND " +
           "p.paymentDate >= :dateFrom AND p.paymentDate < :dateTo " +
           "ORDER BY p.paymentDate DESC")
    Page<Payment> findAllWithFiltersBySpecialty(@Param("searchTerm") String searchTerm,
                                                @Param("paymentMethod") String paymentMethod,
                                                @Param("dateFrom") LocalDateTime dateFrom,
                                                @Param("dateTo") LocalDateTime dateTo,
                                                @Param("specialty") String specialty,
                                                Pageable pageable);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND p.paymentDate >= :startDate AND p.paymentDate < :endDate")
    long countBetweenBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty);

    @Query("SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0), COUNT(p) FROM Payment p " +
           "WHERE p.deleted = false AND p.specialty = :specialty AND p.paymentDate >= :startDate AND p.paymentDate < :endDate " +
           "GROUP BY p.paymentMethod ORDER BY SUM(p.amount) DESC")
    List<Object[]> sumByMethodBetweenBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty);

    @Query("SELECT CAST(p.paymentDate AS LocalDate) AS day, COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.deleted = false AND p.specialty = :specialty AND p.paymentDate >= :startDate AND p.paymentDate < :endDate " +
           "GROUP BY CAST(p.paymentDate AS LocalDate) ORDER BY day")
    List<Object[]> sumDailyIncomeBetweenBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty);

    @Query("SELECT i.clinicalService.name, SUM(i.quantity), SUM(i.totalPrice) FROM PaymentItem i " +
           "WHERE i.payment.deleted = false AND i.payment.specialty = :specialty AND i.clinicalService IS NOT NULL " +
           "AND i.payment.paymentDate >= :startDate AND i.payment.paymentDate < :endDate " +
           "GROUP BY i.clinicalService.name ORDER BY SUM(i.totalPrice) DESC")
    List<Object[]> findTopServicesBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty, Pageable pageable);
}
