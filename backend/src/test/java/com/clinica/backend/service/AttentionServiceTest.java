package com.clinica.backend.service;

import com.clinica.backend.dto.AttentionDto;
import com.clinica.backend.dto.AttentionSummaryDto;
import com.clinica.backend.exception.ResourceNotFoundException;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        clinicalServiceRepository = mock(ClinicalServiceRepository.class);
        clinicalAuthorizationService = mock(ClinicalAuthorizationService.class);
        auditLogService = mock(AuditLogService.class);

        attentionService = new AttentionService(
                attentionRepository,
                patientRepository,
                userRepository,
                appointmentRepository,
                clinicalSessionRepository,
                prescriptionRepository,
                paymentRepository,
                clinicalServiceRepository,
                clinicalAuthorizationService,
                auditLogService
        );

        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("psicologo1");
        currentUser.setFirstName("Juan");
        currentUser.setLastName("Perez");
        currentUser.setSpecialty("PSICOLOGIA");
        currentUser.setRoles(Set.of("ROLE_USER"));

        when(clinicalAuthorizationService.currentUser()).thenReturn(currentUser);

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
        a4.setStatus(Attention.STATUS_COBRADA);

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
        when(paymentRepository.findByIdAndDeletedFalse(60L))
                .thenReturn(Optional.of(payment));
        when(attentionRepository.save(any(Attention.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttentionDto result = attentionService.linkPayment(10L, 60L);

        assertEquals(60L, result.getPaymentId());
        assertEquals(Attention.STATUS_COBRADA, result.getStatus());
        assertEquals(10L, payment.getAttentionId());
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
