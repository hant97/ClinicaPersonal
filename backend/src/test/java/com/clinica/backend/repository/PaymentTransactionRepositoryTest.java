package com.clinica.backend.repository;

import com.clinica.backend.model.Attention;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Payment;
import com.clinica.backend.model.PaymentTransaction;
import com.clinica.backend.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentTransactionRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PaymentTransactionRepository repository;

    @Test
    void sumsOnlyActiveTransactionsByTransactionDateAndAttentionProfessional() {
        User professional = new User();
        professional.setUsername("report-" + UUID.randomUUID());
        professional.setPassword("test-password");
        professional.setFirstName("Ana");
        professional.setLastName("Perez");
        professional.setSpecialty("PSICOLOGIA");
        entityManager.persist(professional);

        Patient patient = new Patient();
        patient.setFirstName("Carlos");
        patient.setLastName("Lopez");
        patient.setGender("MASCULINO");
        patient.setSpecialty("PSICOLOGIA");
        entityManager.persist(patient);

        Payment payment = new Payment();
        payment.setPatient(patient);
        payment.setAmount(new BigDecimal("200.00"));
        payment.setPaymentDate(LocalDateTime.of(2026, 8, 20, 10, 0));
        payment.setSpecialty("PSICOLOGIA");
        entityManager.persist(payment);

        Attention attention = new Attention();
        attention.setPatient(patient);
        attention.setProfessional(professional);
        attention.setPayment(payment);
        attention.setAttentionDate(LocalDate.of(2026, 8, 20));
        attention.setSpecialty("PSICOLOGIA");
        attention.setStatus(Attention.STATUS_ATENDIDA);
        entityManager.persist(attention);
        payment.setAttentionId(attention.getId());

        persistTransaction(payment, new BigDecimal("65.00"), LocalDateTime.of(2026, 9, 1, 0, 0), false);
        persistTransaction(payment, new BigDecimal("20.00"), LocalDateTime.of(2026, 9, 30, 23, 59), false);
        persistTransaction(payment, new BigDecimal("30.00"), LocalDateTime.of(2026, 10, 1, 0, 0), false);
        persistTransaction(payment, new BigDecimal("10.00"), LocalDateTime.of(2026, 9, 15, 12, 0), true);
        entityManager.flush();
        entityManager.clear();

        List<Object[]> rows = repository.sumReceivedByProfessionalBetween(
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 10, 1, 0, 0),
                "PSICOLOGIA");

        assertEquals(1, rows.size());
        assertEquals(professional.getId(), ((Number) rows.get(0)[0]).longValue());
        assertEquals("Ana", rows.get(0)[1]);
        assertEquals("Perez", rows.get(0)[2]);
        assertEquals(professional.getUsername(), rows.get(0)[3]);
        assertEquals(0, new BigDecimal("85.00").compareTo((BigDecimal) rows.get(0)[4]));
    }

    private void persistTransaction(Payment payment, BigDecimal amount, LocalDateTime date, boolean deleted) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setDeleted(deleted);
        entityManager.persist(transaction);
    }
}
