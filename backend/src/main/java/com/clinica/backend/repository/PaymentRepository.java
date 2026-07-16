package com.clinica.backend.repository;

import com.clinica.backend.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByPatientIdAndDeletedFalseOrderByPaymentDateDesc(Long patientId, Pageable pageable);

    Page<Payment> findAllByDeletedFalseOrderByPaymentDateDesc(Pageable pageable);
}
