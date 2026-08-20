package com.clinica.backend.repository;

import com.clinica.backend.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByIdAndDeletedFalse(Long id);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
           "WHERE t.deleted = false AND t.payment.deleted = false AND t.payment.specialty = :specialty " +
           "AND t.transactionDate >= :startDate AND t.transactionDate < :endDate")
    BigDecimal sumIncomeBetweenBySpecialty(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           @Param("specialty") String specialty);

    @Query("SELECT COUNT(t) FROM PaymentTransaction t " +
           "WHERE t.deleted = false AND t.payment.deleted = false AND t.payment.specialty = :specialty " +
           "AND t.transactionDate >= :startDate AND t.transactionDate < :endDate")
    long countBetweenBySpecialty(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 @Param("specialty") String specialty);

    @Query("SELECT t.paymentMethod, COALESCE(SUM(t.amount), 0), COUNT(t) FROM PaymentTransaction t " +
           "WHERE t.deleted = false AND t.payment.deleted = false AND t.payment.specialty = :specialty " +
           "AND t.transactionDate >= :startDate AND t.transactionDate < :endDate " +
           "GROUP BY t.paymentMethod ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumByMethodBetweenBySpecialty(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("specialty") String specialty);

    @Query("SELECT CAST(t.transactionDate AS LocalDate) AS day, COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
           "WHERE t.deleted = false AND t.payment.deleted = false AND t.payment.specialty = :specialty " +
           "AND t.transactionDate >= :startDate AND t.transactionDate < :endDate " +
           "GROUP BY CAST(t.transactionDate AS LocalDate) ORDER BY day")
    List<Object[]> sumDailyIncomeBetweenBySpecialty(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate,
                                                    @Param("specialty") String specialty);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
           "WHERE t.deleted = false AND t.payment.deleted = false AND t.payment.specialty = :specialty")
    BigDecimal sumReceivedBySpecialty(@Param("specialty") String specialty);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t " +
           "WHERE t.deleted = false AND t.payment.deleted = false " +
           "AND t.payment.specialty = :specialty AND t.payment.patient.id = :patientId")
    BigDecimal sumReceivedByPatientAndSpecialty(@Param("patientId") Long patientId,
                                                @Param("specialty") String specialty);
}
