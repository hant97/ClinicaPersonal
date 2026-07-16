package com.clinica.backend.repository;
import com.clinica.backend.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPatientIdAndDeletedFalseOrderByPaymentDateDesc(Long patientId);
    Page<Payment> findAllByDeletedFalseOrderByPaymentDateDesc(Pageable pageable);
    List<Payment> findAllByDeletedFalseOrderByPaymentDateDesc();
}
