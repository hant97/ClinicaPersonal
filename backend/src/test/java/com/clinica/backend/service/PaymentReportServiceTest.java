package com.clinica.backend.service;

import com.clinica.backend.dto.PatientBalanceDto;
import com.clinica.backend.dto.PaymentSummaryDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentReportServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentTransactionRepository transactionRepository;
    private PatientRepository patientRepository;
    private PaymentReportService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        transactionRepository = mock(PaymentTransactionRepository.class);
        patientRepository = mock(PatientRepository.class);
        service = new PaymentReportService(paymentRepository, transactionRepository, patientRepository);

        User user = new User();
        user.setSpecialty("PSICOLOGIA");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        when(paymentRepository.sumChargedBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.sumReceivedBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("350.00"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsInvertedOrTooLongRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getSummary(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.getSummary(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 9, 1)));
    }

    @Test
    void summarizesCustomRangeWithGrowthAverageAndOneChartPointPerDay() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 10);
        // Periodo actual: 300 en 3 cobros; periodo anterior de igual duración: 200.
        when(transactionRepository.sumIncomeBetweenBySpecialty(eq(from.atStartOfDay()), eq(to.plusDays(1).atStartOfDay()), eq("PSICOLOGIA")))
                .thenReturn(new BigDecimal("300.00"));
        when(transactionRepository.sumIncomeBetweenBySpecialty(eq(from.minusDays(10).atStartOfDay()), eq(from.atStartOfDay()), eq("PSICOLOGIA")))
                .thenReturn(new BigDecimal("200.00"));
        when(transactionRepository.countBetweenBySpecialty(any(), any(), eq("PSICOLOGIA"))).thenReturn(3L);
        when(transactionRepository.sumDailyIncomeBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 9, 5), new BigDecimal("300.00")}));
        when(transactionRepository.sumByMethodBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.<Object[]>of(new Object[]{"CASH", new BigDecimal("300.00"), 3L}));

        PaymentSummaryDto summary = service.getSummary(from, to);

        assertEquals(new BigDecimal("300.00"), summary.getIncomeMonth());
        assertEquals(50, summary.getMonthlyGrowth());
        assertEquals(new BigDecimal("100.00"), summary.getAverageTicket());
        assertEquals(new BigDecimal("150.00"), summary.getPendingBalance());
        assertEquals(10, summary.getDailyIncome().size());
        assertEquals(new BigDecimal("300.00"), summary.getDailyIncome().get(4).getTotal());
        assertEquals(BigDecimal.ZERO, summary.getDailyIncome().get(0).getTotal());
        assertEquals("CASH", summary.getMethodBreakdown().get(0).getMethod());
    }

    @Test
    void patientBalanceIsNeverNegativeAndIsScopedToTheSpecialty() {
        Patient patient = new Patient();
        patient.setId(9L);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(9L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(paymentRepository.sumChargedByPatientAndSpecialty(9L, "PSICOLOGIA")).thenReturn(new BigDecimal("100.00"));
        when(transactionRepository.sumReceivedByPatientAndSpecialty(9L, "PSICOLOGIA")).thenReturn(new BigDecimal("120.00"));

        PatientBalanceDto balance = service.getPatientBalance(9L);

        assertEquals(0, balance.getBalance().compareTo(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.getPatientBalance(10L));
    }
}
