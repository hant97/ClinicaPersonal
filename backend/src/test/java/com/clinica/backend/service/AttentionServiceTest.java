package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.dto.AttentionSummaryDto;
import com.clinica.backend.dto.ProfessionalProductivityDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AttentionMapperImpl;
import com.clinica.backend.model.*;
import com.clinica.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AttentionServiceTest {

    private AttentionRepository attentionRepository;
    private PatientRepository patientRepository;
    private UserRepository userRepository;
    private AppointmentRepository appointmentRepository;
    private ClinicalSessionRepository clinicalSessionRepository;
    private PrescriptionRepository prescriptionRepository;
    private PaymentRepository paymentRepository;
    private PaymentTransactionRepository paymentTransactionRepository;
    private ClinicalServiceRepository clinicalServiceRepository;
    private ClinicalAuthorizationService clinicalAuthorizationService;
    private AuditLogService auditLogService;
    private AttentionService attentionService;

    private User currentUser;
    private Patient patient;

    @BeforeEach
    void setUp() {
        attentionRepository = mock(AttentionRepository.class);
        patientRepository = mock(PatientRepository.class);
        userRepository = mock(UserRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        clinicalSessionRepository = mock(ClinicalSessionRepository.class);
        prescriptionRepository = mock(PrescriptionRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        clinicalServiceRepository = mock(ClinicalServiceRepository.class);
        clinicalAuthorizationService = mock(ClinicalAuthorizationService.class);
        auditLogService = mock(AuditLogService.class);

        attentionService = new AttentionService(
                attentionRepository,
                patientRepository,
                userRepository,
                appointmentRepository,
                clinicalSessionRepository,
                new AttentionReportService(attentionRepository, paymentTransactionRepository, clinicalAuthorizationService),
                new AttentionStatusService(attentionRepository, appointmentRepository,
                        clinicalAuthorizationService, auditLogService, new AttentionMapperImpl()),
                new AttentionLinkService(attentionRepository, clinicalSessionRepository,
                        prescriptionRepository, paymentRepository, clinicalAuthorizationService,
                        auditLogService, new AttentionMapperImpl()),
                clinicalServiceRepository,
                clinicalAuthorizationService,
                auditLogService,
                new AttentionMapperImpl()
        );

        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("psicologo1");
        currentUser.setFirstName("Juan");
        currentUser.setLastName("Perez");
        currentUser.setSpecialty("PSICOLOGIA");
        currentUser.setRoles(Set.of("ROLE_PROFESIONAL"));

        when(clinicalAuthorizationService.currentUser()).thenReturn(currentUser);
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );

        patient = new Patient();
        patient.setId(100L);
        patient.setFirstName("Carlos");
        patient.setLastName("Gomez");
        patient.setIdentificationDocument("12345678");
        patient.setSpecialty("PSICOLOGIA");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchAttentions_returnsMappedPage() {
        Attention attention = new Attention();
        attention.setId(1L);
        attention.setPatient(patient);
        attention.setProfessional(currentUser);
        attention.setSpecialty("PSICOLOGIA");
        attention.setAttentionDate(LocalDate.now());
        attention.setStatus(Attention.STATUS_EN_PROCESO);

        when(attentionRepository.searchAttentions(
                eq("PSICOLOGIA"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(attention)));

        Page<AttentionDto> result = attentionService.searchAttentions(
                null, null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        AttentionDto dto = result.getContent().get(0);
        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getPatientId());
        assertEquals("Carlos Gomez", dto.getPatientName());
        assertEquals("12345678", dto.getPatientDocumentNumber());
        assertEquals(10L, dto.getProfessionalId());
        assertEquals("Juan Perez", dto.getProfessionalName());
        assertEquals(Attention.STATUS_EN_PROCESO, dto.getStatus());
    }

    @Test
    void searchAttentions_normalizesEmptySearchTermAndStatusAll() {
        Attention attention = new Attention();
        attention.setId(1L);
        attention.setPatient(patient);
        attention.setProfessional(currentUser);
        attention.setSpecialty("PSICOLOGIA");
        attention.setAttentionDate(LocalDate.now());
        attention.setStatus(Attention.STATUS_AGENDADA);

        when(attentionRepository.searchAttentions(
                eq("PSICOLOGIA"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(attention)));

        Page<AttentionDto> result = attentionService.searchAttentions(
                "   ", "ALL", null, null, null, null, PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        verify(attentionRepository).searchAttentions(
                eq("PSICOLOGIA"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)
        );
    }

    @Test
    void getAttentionById_success() {
        Attention attention = new Attention();
        attention.setId(2L);
        attention.setPatient(patient);
        attention.setProfessional(currentUser);
        attention.setSpecialty("PSICOLOGIA");
        attention.setAttentionDate(LocalDate.now());
        attention.setStatus(Attention.STATUS_ATENDIDA);

        when(attentionRepository.findByIdAndSpecialtyAndDeletedFalse(2L, "PSICOLOGIA"))
                .thenReturn(Optional.of(attention));

        AttentionDto dto = attentionService.getAttentionById(2L);

        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertEquals(Attention.STATUS_ATENDIDA, dto.getStatus());
    }

    @Test
    void getAttentionById_notFound_throwsException() {
        when(attentionRepository.findByIdAndSpecialtyAndDeletedFalse(99L, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attentionService.getAttentionById(99L));
    }

    @Test
    void getTodaySummary_calculatesMetricsAccurately() {
        Attention a1 = new Attention();
        a1.setStatus(Attention.STATUS_AGENDADA);

        Attention a2 = new Attention();
        a2.setStatus(Attention.STATUS_EN_PROCESO);

        ClinicalService service = new ClinicalService();
        service.setPrice(new BigDecimal("150.00"));

        Attention a3 = new Attention();
        a3.setStatus(Attention.STATUS_ATENDIDA);
        a3.setClinicalService(service);

        Attention a4 = new Attention();
        a4.setStatus(Attention.STATUS_ATENDIDA);
        Payment fullyPaid = new Payment();
        fullyPaid.setAmount(new BigDecimal("100.00"));
        fullyPaid.setStatus(Payment.STATUS_PAGADO);
        PaymentTransaction fullPayment = new PaymentTransaction();
        fullPayment.setAmount(new BigDecimal("100.00"));
        fullyPaid.setTransactions(List.of(fullPayment));
        a4.setPayment(fullyPaid);

        when(attentionRepository.findByAttentionDateAndSpecialtyAndDeletedFalse(LocalDate.now(), "PSICOLOGIA"))
                .thenReturn(List.of(a1, a2, a3, a4));

        AttentionSummaryDto summary = attentionService.getTodaySummary();

        assertEquals(4, summary.getTotalToday());
        assertEquals(1, summary.getScheduledToday());
        assertEquals(1, summary.getInProgressToday());
        assertEquals(1, summary.getAttendedToday());
        assertEquals(1, summary.getPaidToday());
        assertEquals(0, summary.getCancelledToday());
        assertEquals(new BigDecimal("150.00"), summary.getPendingBillingAmount());
    }

    @Test
    void getTodaySummary_subtractsPartialPaymentsFromOutstandingAmount() {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("200.00"));
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAmount(new BigDecimal("65.00"));
        payment.setTransactions(List.of(transaction));

        Attention attention = new Attention();
        attention.setStatus(Attention.STATUS_ATENDIDA);
        attention.setPayment(payment);
        when(attentionRepository.findByAttentionDateAndSpecialtyAndDeletedFalse(LocalDate.now(), "PSICOLOGIA"))
                .thenReturn(List.of(attention));

        assertEquals(new BigDecimal("135.00"), attentionService.getTodaySummary().getPendingBillingAmount());
    }

    @Test
    void getProfessionalProductivity_usesInvoiceAmountAndPaymentsInSelectedDates() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ClinicalService service = new ClinicalService();
        service.setPrice(new BigDecimal("300.00"));
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("200.00"));
        payment.setStatus(Payment.STATUS_PARCIAL);
        Attention attention = new Attention();
        attention.setProfessional(currentUser);
        attention.setStatus(Attention.STATUS_ATENDIDA);
        attention.setClinicalService(service);
        attention.setPayment(payment);
        when(attentionRepository.findBySpecialtyAndAttentionDateBetweenAndDeletedFalse("PSICOLOGIA", from, to))
                .thenReturn(List.of(attention));
        when(paymentTransactionRepository.sumReceivedByProfessionalBetween(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0), "PSICOLOGIA"))
                .thenReturn(List.<Object[]>of(new Object[]{10L, "Juan", "Perez", "psicologo1", new BigDecimal("65.00")}));

        List<ProfessionalProductivityDto> report = attentionService.getProfessionalProductivity(from, to);

        assertEquals(1, report.size());
        assertEquals(new BigDecimal("200.00"), report.get(0).getBilledAmount());
        assertEquals(new BigDecimal("65.00"), report.get(0).getCollectedAmount());
        assertEquals(1, report.get(0).getAttendedAttentions());
        assertEquals(0, report.get(0).getPaidAttentions());
    }

    @Test
    void getProfessionalProductivity_includesCollectionsForOlderAttentions() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(attentionRepository.findBySpecialtyAndAttentionDateBetweenAndDeletedFalse("PSICOLOGIA", from, to))
                .thenReturn(List.of());
        when(paymentTransactionRepository.sumReceivedByProfessionalBetween(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.<Object[]>of(new Object[]{10L, "Juan", "Perez", "psicologo1", new BigDecimal("80.00")}));

        ProfessionalProductivityDto row = attentionService.getProfessionalProductivity(from, to).get(0);

        assertEquals(0, row.getTotalAttentions());
        assertEquals(BigDecimal.ZERO, row.getBilledAmount());
        assertEquals(new BigDecimal("80.00"), row.getCollectedAmount());
    }

    @Test
    void getProfessionalProductivity_countsFullyPaidPaymentsEvenWhenAttentionStatusIsStale() {
        LocalDate day = LocalDate.of(2026, 9, 15);
        Payment paidPayment = new Payment();
        paidPayment.setAmount(new BigDecimal("100.00"));
        paidPayment.setStatus(Payment.STATUS_PAGADO);
        Attention paidAttention = new Attention();
        paidAttention.setProfessional(currentUser);
        paidAttention.setStatus(Attention.STATUS_ATENDIDA);
        paidAttention.setPayment(paidPayment);

        Payment deletedPayment = new Payment();
        deletedPayment.setAmount(new BigDecimal("100.00"));
        deletedPayment.setStatus(Payment.STATUS_PAGADO);
        deletedPayment.setDeleted(true);
        Attention deletedPaymentAttention = new Attention();
        deletedPaymentAttention.setProfessional(currentUser);
        deletedPaymentAttention.setStatus(Attention.STATUS_COBRADA);
        deletedPaymentAttention.setPayment(deletedPayment);
        when(attentionRepository.findBySpecialtyAndAttentionDateBetweenAndDeletedFalse("PSICOLOGIA", day, day))
                .thenReturn(List.of(paidAttention, deletedPaymentAttention));

        ProfessionalProductivityDto row = attentionService.getProfessionalProductivity(day, day).get(0);

        assertEquals(1, row.getPaidAttentions());
        assertEquals(new BigDecimal("100.00"), row.getBilledAmount());
    }

    @Test
    void create_quickAttention_savesAndLogs() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        Attention saved = new Attention();
        saved.setId(5L);
        saved.setPatient(patient);
        saved.setProfessional(currentUser);
        saved.setSpecialty("PSICOLOGIA");
        saved.setAttentionDate(LocalDate.now());
        saved.setStatus(Attention.STATUS_EN_PROCESO);
        saved.setStartTime(LocalTime.of(10, 0));

        when(attentionRepository.save(any(Attention.class))).thenReturn(saved);

        AttentionDto input = new AttentionDto();
        input.setPatientId(100L);
        input.setAttentionDate(LocalDate.now());
        input.setStatus(Attention.STATUS_EN_PROCESO);
        input.setMotive("Consulta de urgencia");

        AttentionDto result = attentionService.create(input);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(Attention.STATUS_EN_PROCESO, result.getStatus());
        verify(auditLogService).record(eq("CREATE"), eq("ATTENTION"), eq("5"), anyString());
    }

    @Test
    void create_rejectsUnknownProfessionalWithoutSaving() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        AttentionDto input = new AttentionDto();
        input.setPatientId(100L);
        input.setProfessionalId(99L);

        assertThrows(ResourceNotFoundException.class, () -> attentionService.create(input));
        verify(attentionRepository, never()).save(any());
    }

    @Test
    void create_rejectsProfessionalFromAnotherSpecialty() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        User professional = professional(20L, "DERMATOLOGIA", true, Set.of("ROLE_PROFESIONAL"));
        when(userRepository.findById(20L)).thenReturn(Optional.of(professional));
        AttentionDto input = new AttentionDto();
        input.setPatientId(100L);
        input.setProfessionalId(20L);

        assertThrows(AccessDeniedException.class, () -> attentionService.create(input));
        verify(attentionRepository, never()).save(any());
    }

    @Test
    void create_rejectsInactiveOrNonProfessionalUser() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        AttentionDto input = new AttentionDto();
        input.setPatientId(100L);
        input.setProfessionalId(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(
                professional(20L, "PSICOLOGIA", false, Set.of("ROLE_PROFESIONAL"))));
        assertThrows(IllegalArgumentException.class, () -> attentionService.create(input));

        when(userRepository.findById(20L)).thenReturn(Optional.of(
                professional(20L, "PSICOLOGIA", true, Set.of("ROLE_ADMIN"))));
        assertThrows(IllegalArgumentException.class, () -> attentionService.create(input));
        verify(attentionRepository, never()).save(any());
    }

    @Test
    void create_assignsValidProfessionalFromSameSpecialty() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        User professional = professional(20L, "PSICOLOGIA", true, Set.of("ROLE_PROFESIONAL"));
        when(userRepository.findById(20L)).thenReturn(Optional.of(professional));
        when(attentionRepository.save(any(Attention.class))).thenAnswer(invocation -> {
            Attention attention = invocation.getArgument(0);
            attention.setId(5L);
            return attention;
        });
        AttentionDto input = new AttentionDto();
        input.setPatientId(100L);
        input.setProfessionalId(20L);

        assertEquals(20L, attentionService.create(input).getProfessionalId());
    }

    private User professional(Long id, String specialty, boolean enabled, Set<String> roles) {
        User professional = new User();
        professional.setId(id);
        professional.setSpecialty(specialty);
        professional.setEnabled(enabled);
        professional.setRoles(roles);
        return professional;
    }

    @Test
    void createFromAppointment_createsAndLinksAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(30L);
        appointment.setPatient(patient);
        appointment.setProfessionalId(10L);
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(14, 0));
        appointment.setNotes("Terapia semanal");

        when(appointmentRepository.findByIdAndSpecialty(30L, "PSICOLOGIA"))
                .thenReturn(Optional.of(appointment));
        when(attentionRepository.findByAppointmentIdAndDeletedFalse(30L))
                .thenReturn(Optional.empty());

        Attention saved = new Attention();
        saved.setId(7L);
        saved.setPatient(patient);
        saved.setProfessional(currentUser);
        saved.setSpecialty("PSICOLOGIA");
        saved.setAppointment(appointment);
        saved.setAttentionDate(LocalDate.now());
        saved.setStartTime(LocalTime.of(14, 0));
        saved.setStatus(Attention.STATUS_EN_PROCESO);

        when(attentionRepository.save(any(Attention.class))).thenReturn(saved);

        AttentionDto result = attentionService.createFromAppointment(30L);

        assertNotNull(result);
        assertEquals(7L, result.getId());
        assertEquals(Attention.STATUS_EN_PROCESO, result.getStatus());
        assertEquals(30L, result.getAppointmentId());
        assertEquals(7L, appointment.getAttentionId());
        verify(appointmentRepository).save(appointment);
        verify(auditLogService).record(eq("CREATE"), eq("ATTENTION"), eq("7"), anyString());
    }

    @Test
    void createFromAppointment_rejectsUnknownProfessionalWithoutFallback() {
        Appointment appointment = new Appointment();
        appointment.setId(30L);
        appointment.setProfessionalId(99L);
        when(appointmentRepository.findByIdAndSpecialty(30L, "PSICOLOGIA"))
                .thenReturn(Optional.of(appointment));

        assertThrows(ResourceNotFoundException.class, () -> attentionService.createFromAppointment(30L));
        verify(attentionRepository, never()).save(any());
    }

    @Test
    void updateStatus_transitionsToAttended_calculatesDurationAndCompletesAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(40L);
        appointment.setStatus("PROGRAMADA");

        Attention attention = new Attention();
        attention.setId(8L);
        attention.setPatient(patient);
        attention.setProfessional(currentUser);
        attention.setSpecialty("PSICOLOGIA");
        attention.setAttentionDate(LocalDate.now());
        attention.setStartTime(LocalTime.of(10, 0));
        attention.setStatus(Attention.STATUS_EN_PROCESO);
        attention.setAppointment(appointment);

        when(attentionRepository.findByIdAndSpecialtyAndDeletedFalse(8L, "PSICOLOGIA"))
                .thenReturn(Optional.of(attention));
        when(attentionRepository.save(any(Attention.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttentionDto result = attentionService.updateStatus(8L, Attention.STATUS_ATENDIDA, "Paciente evolucionó favorablemente");

        assertEquals(Attention.STATUS_ATENDIDA, result.getStatus());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getDurationMinutes());
        assertTrue(result.getDurationMinutes() >= 0);
        assertEquals("COMPLETADA", appointment.getStatus());
        verify(appointmentRepository).save(appointment);
        verify(auditLogService).record(eq("UPDATE"), eq("ATTENTION"), eq("8"), anyString());
    }

    @Test
    void linkClinicalSession_linksSessionAndAttention() {
        Attention attention = new Attention();
        attention.setId(9L);
        attention.setSpecialty("PSICOLOGIA");
        attention.setPatient(patient);
        attention.setProfessional(currentUser);

        ClinicalSession session = new ClinicalSession();
        session.setId(50L);

        when(attentionRepository.findByIdAndSpecialtyAndDeletedFalse(9L, "PSICOLOGIA"))
                .thenReturn(Optional.of(attention));
        when(clinicalSessionRepository.findByIdAndDeletedFalse(50L))
                .thenReturn(Optional.of(session));
        when(attentionRepository.save(any(Attention.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttentionDto result = attentionService.linkClinicalSession(9L, 50L);

        assertEquals(50L, result.getClinicalSessionId());
        assertEquals(9L, session.getAttentionId());
        verify(clinicalSessionRepository).save(session);
    }

    @Test
    void linkPayment_transitionsToCobradaWhenPaid() {
        Attention attention = new Attention();
        attention.setId(10L);
        attention.setSpecialty("PSICOLOGIA");
        attention.setPatient(patient);
        attention.setProfessional(currentUser);
        attention.setStatus(Attention.STATUS_ATENDIDA);

        Payment payment = new Payment();
        payment.setId(60L);
        payment.setStatus(Payment.STATUS_PAGADO);
        payment.setAmount(new BigDecimal("120.00"));

        when(attentionRepository.findByIdAndSpecialtyAndDeletedFalse(10L, "PSICOLOGIA"))
                .thenReturn(Optional.of(attention));
        when(paymentRepository.findByIdForUpdate(60L))
                .thenReturn(Optional.of(payment));
        when(attentionRepository.save(any(Attention.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttentionDto result = attentionService.linkPayment(10L, 60L);

        assertEquals(60L, result.getPaymentId());
        assertEquals(Attention.STATUS_COBRADA, result.getStatus());
        assertEquals(10L, payment.getAttentionId());
        verify(paymentRepository).findByIdForUpdate(60L);
        verify(paymentRepository).save(payment);
    }

    @Test
    void delete_softDeletesAndLogs() {
        Attention attention = new Attention();
        attention.setId(11L);
        attention.setSpecialty("PSICOLOGIA");
        attention.setPatient(patient);
        attention.setProfessional(currentUser);

        when(attentionRepository.findByIdAndSpecialtyAndDeletedFalse(11L, "PSICOLOGIA"))
                .thenReturn(Optional.of(attention));

        attentionService.delete(11L);

        assertTrue(attention.isDeleted());
        assertNotNull(attention.getDeletedAt());
        assertEquals(10L, attention.getDeletedBy());
        verify(attentionRepository).save(attention);
        verify(auditLogService).record(eq("DELETE"), eq("ATTENTION"), eq("11"), anyString());
    }
}
