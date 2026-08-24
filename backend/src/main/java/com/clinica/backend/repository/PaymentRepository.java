package com.clinica.backend.repository;

import com.clinica.backend.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdAndDeletedFalse(Long id);
    Optional<Payment> findByIdAndSpecialtyAndDeletedFalse(Long id, String specialty);

    @EntityGraph(attributePaths = {"patient", "appointment", "clinicalSession"})
    Page<Payment> findByPatientIdAndSpecialtyAndDeletedFalseOrderByPaymentDateDesc(Long patientId, String specialty, Pageable pageable);

    Optional<Payment> findFirstByAppointmentIdAndDeletedFalse(Long appointmentId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.appointment a " +
           "WHERE a.id IN :appointmentIds AND p.deleted = false")
    List<Payment> findByAppointmentIdInAndDeletedFalse(@Param("appointmentIds") Collection<Long> appointmentIds);

    @EntityGraph(attributePaths = {"patient", "appointment", "clinicalSession"})
    @Query("SELECT p FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND " +
           "(cast(:searchTerm as string) IS NULL OR cast(:searchTerm as string) = '' OR " +
           "LOWER(p.patient.firstName) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%')) OR " +
           "LOWER(p.patient.lastName) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', cast(:searchTerm as string), '%'))) AND " +
           "(cast(:paymentMethod as string) IS NULL OR cast(:paymentMethod as string) = '' OR p.paymentMethod = :paymentMethod) AND " +
           "(cast(:status as string) IS NULL OR cast(:status as string) = '' OR p.status = :status) AND " +
           "p.paymentDate >= :dateFrom AND p.paymentDate < :dateTo " +
           "ORDER BY p.paymentDate DESC")
    Page<Payment> findAllWithFiltersBySpecialty(@Param("searchTerm") String searchTerm,
                                                @Param("paymentMethod") String paymentMethod,
                                                @Param("status") String status,
                                                @Param("dateFrom") LocalDateTime dateFrom,
                                                @Param("dateTo") LocalDateTime dateTo,
                                                @Param("specialty") String specialty,
                                                Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty")
    BigDecimal sumChargedBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deleted = false AND p.specialty = :specialty AND p.patient.id = :patientId")
    BigDecimal sumChargedByPatientAndSpecialty(@Param("patientId") Long patientId, @Param("specialty") String specialty);

    @Query("SELECT i.clinicalService.name, SUM(i.quantity), SUM(i.totalPrice) FROM PaymentItem i " +
           "WHERE i.payment.deleted = false AND i.payment.specialty = :specialty AND i.clinicalService IS NOT NULL " +
           "AND i.payment.paymentDate >= :startDate AND i.payment.paymentDate < :endDate " +
           "GROUP BY i.clinicalService.name ORDER BY SUM(i.totalPrice) DESC")
    List<Object[]> findTopServicesBySpecialty(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("specialty") String specialty, Pageable pageable);
}
