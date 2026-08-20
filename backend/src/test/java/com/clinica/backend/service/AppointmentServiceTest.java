package com.clinica.backend.service;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PaymentRepository;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.model.Attention;
import com.clinica.backend.repository.AttentionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AttentionRepository attentionRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalServiceRepository clinicalServiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GoogleCalendarService googleCalendarService;
    @Mock
    private ProfessionalScheduleService professionalScheduleService;
    @Mock
    private ScheduleBlockService scheduleBlockService;

    @InjectMocks
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("doctor");
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAppointmentShouldSucceedWhenNoOverlap() {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId(5L);
        dto.setAppointmentDate(LocalDate.of(2026, 9, 1));
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(11, 0));
        dto.setProfessionalId(1L);

        Patient patient = new Patient();
        patient.setId(5L);

        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(5L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));
        when(appointmentRepository.findOverlappingAppointments(
                dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), "PSICOLOGIA", 1L))
                .thenReturn(List.of());

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        AppointmentDto created = appointmentService.create(dto);
        assertNotNull(created);
        assertEquals(100L, created.getId());
        assertEquals("PROGRAMADA", created.getStatus());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void createAppointmentShouldFailWhenTimesOverlap() {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId(5L);
        dto.setAppointmentDate(LocalDate.of(2026, 9, 1));
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(11, 0));
        dto.setProfessionalId(1L);

        Patient patient = new Patient();
        patient.setId(5L);

        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(5L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        Appointment existing = new Appointment();
        existing.setId(50L);
        existing.setStartTime(LocalTime.of(10, 30));
        existing.setEndTime(LocalTime.of(11, 30));

        when(appointmentRepository.findOverlappingAppointments(
                dto.getAppointmentDate(), dto.getStartTime(), dto.getEndTime(), "PSICOLOGIA", 1L))
                .thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.create(dto)
        );
        assertTrue(ex.getMessage().contains("Ya existe una cita programada"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointmentShouldFailWhenStartTimeAfterEndTime() {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId(5L);
        dto.setAppointmentDate(LocalDate.of(2026, 9, 1));
        dto.setStartTime(LocalTime.of(11, 0));
        dto.setEndTime(LocalTime.of(10, 0));

        Patient patient = new Patient();
        patient.setId(5L);

        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(5L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(dto));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void mapToDtoShouldIncludePaymentInfoWhenPaid() {
        Appointment appointment = new Appointment();
        appointment.setId(100L);
        Patient patient = new Patient();
        patient.setId(5L);
        patient.setFirstName("Juan");
        patient.setLastName("Perez");
        appointment.setPatient(patient);
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(10, 0));
        appointment.setStatus("COMPLETADA");

        com.clinica.backend.model.Payment payment = new com.clinica.backend.model.Payment();
        payment.setId(88L);
        payment.setAmount(new java.math.BigDecimal("150.00"));
        payment.setAppointment(appointment);

        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(5L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByPatientIdAndSpecialtyOrderByAppointmentDateDescStartTimeDesc(eq(5L), eq("PSICOLOGIA"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(appointment)));
        when(paymentRepository.findByAppointmentIdInAndDeletedFalse(List.of(100L))).thenReturn(List.of(payment));

        org.springframework.data.domain.Page<AppointmentDto> page = appointmentService.getByPatientId(5L, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        AppointmentDto result = page.getContent().get(0);
        assertTrue(result.isPaid());
        assertEquals(88L, result.getPaymentId());
        assertEquals(new java.math.BigDecimal("150.00"), result.getPaymentAmount());
    }

    @Test
    void updateStatusToConfirmadaShouldTriggerGoogleCalendarSync() {
        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setStatus("PROGRAMADA");
        appointment.setSpecialty("PSICOLOGIA");
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setFirstName("Ana");
        patient.setLastName("Gomez");
        appointment.setPatient(patient);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentDto result = appointmentService.updateStatus(10L, "CONFIRMADA");

        assertEquals("CONFIRMADA", result.getStatus());
        verify(googleCalendarService).syncAppointment(appointment);
    }

    @Test
    void updateStatusToCanceladaShouldTriggerGoogleCalendarCancellation() {
        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setStatus("PROGRAMADA");
        appointment.setSpecialty("PSICOLOGIA");
        appointment.setGoogleEventId("google-evt-123");
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setFirstName("Ana");
        patient.setLastName("Gomez");
        appointment.setPatient(patient);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentDto result = appointmentService.updateStatus(10L, "CANCELADA");

        assertEquals("CANCELADA", result.getStatus());
        verify(googleCalendarService).cancelAppointmentEvent(appointment);
    }

    @Test
    void updateStatusToConfirmadaShouldCreateAttentionInAgendadaState() {
        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setStatus("PROGRAMADA");
        appointment.setSpecialty("PSICOLOGIA");
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setFirstName("Ana");
        patient.setLastName("Gomez");
        appointment.setPatient(patient);
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(14, 0));
        appointment.setEndTime(LocalTime.of(14, 30));

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(attentionRepository.findByAppointmentIdAndDeletedFalse(10L)).thenReturn(Optional.empty());
        when(attentionRepository.save(any(Attention.class))).thenAnswer(i -> {
            Attention a = i.getArgument(0);
            a.setId(999L);
            return a;
        });
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentDto result = appointmentService.updateStatus(10L, "CONFIRMADA");

        assertEquals("CONFIRMADA", result.getStatus());
        verify(attentionRepository).save(argThat(att ->
                "AGENDADA".equals(att.getStatus()) &&
                LocalDate.now().equals(att.getAttentionDate()) &&
                LocalTime.of(14, 0).equals(att.getStartTime()) &&
                patient.equals(att.getPatient())
        ));
        assertEquals(999L, appointment.getAttentionId());
    }
}
