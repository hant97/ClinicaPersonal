package com.clinica.backend.service;

import com.clinica.backend.dto.DashboardStatsDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.AssessmentRepository;
import com.clinica.backend.repository.CatalogRepository;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.DermatologicalEvaluationRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.PaymentTransactionRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private RiskAlertRepository riskAlertRepository;
    @Mock
    private SupplyService supplyService;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private DermatologicalEvaluationRepository dermatologicalEvaluationRepository;
    @Mock
    private CatalogRepository catalogRepository;
    @Mock
    private ClinicalSessionRepository clinicalSessionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void dashboardExcludesCancelledAppointmentsFromTodayListAndCount() {
        Appointment cancelled = appointment(1L, "CANCELADA", "Carlos");
        Appointment confirmed = appointment(2L, "CONFIRMADA", "Lucía");

        when(paymentTransactionRepository.sumIncomeBetweenBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(BigDecimal.ZERO);
        when(appointmentRepository.findByAppointmentDateBetweenAndSpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.of(cancelled, confirmed));
        when(appointmentRepository.findUpcomingAppointmentsBySpecialty(any(), any(), eq("PSICOLOGIA"), any(Pageable.class)))
                .thenReturn(List.of());
        when(appointmentRepository.findTodayAppointmentsBySpecialty(any(), eq("PSICOLOGIA")))
                .thenReturn(List.of(cancelled, confirmed));
        when(paymentRepository.findByAppointmentIdInAndDeletedFalse(anyCollection()))
                .thenReturn(List.of());
        when(riskAlertRepository.findBySpecialtyAndActiveTrueOrderByCreatedAtDesc(eq("PSICOLOGIA"), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(supplyService.getLowStockSupplies()).thenReturn(List.of());
        when(clinicalSessionRepository.findPendingNotesBySpecialty(eq("PSICOLOGIA"), any(), any(Pageable.class)))
                .thenReturn(List.of());

        DashboardStatsDto result = dashboardService.getDashboardStats("PSICOLOGIA");

        assertEquals(1, result.getAppointmentsToday());
        assertEquals(List.of(2L), result.getTodaysAppointments().stream()
                .map(appointment -> appointment.getId())
                .toList());
    }

    private Appointment appointment(Long id, String status, String patientName) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFirstName(patientName);

        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setStatus(status);
        appointment.setSpecialty("PSICOLOGIA");
        return appointment;
    }
}
