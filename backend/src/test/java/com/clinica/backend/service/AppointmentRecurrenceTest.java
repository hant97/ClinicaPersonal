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
import com.clinica.backend.repository.AttentionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRecurrenceTest {

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

    private User currentUser;
    private Patient patient;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("dra.ana");
        currentUser.setFirstName("Ana");
        currentUser.setLastName("Rivas");
        currentUser.setSpecialty("PSICOLOGIA");
        currentUser.setRoles(Set.of("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );

        patient = new Patient();
        patient.setId(100L);
        patient.setFirstName("María");
        patient.setLastName("Pérez");
        patient.setSpecialty("PSICOLOGIA");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRecurringAppointmentGeneratesWeeklySeriesWithSameGroupId() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        AppointmentDto inputDto = new AppointmentDto();
        inputDto.setPatientId(100L);
        inputDto.setAppointmentDate(LocalDate.of(2026, 9, 1)); // Tuesday
        inputDto.setStartTime(LocalTime.of(10, 0));
        inputDto.setEndTime(LocalTime.of(11, 0));
        inputDto.setStatus("PROGRAMADA");
        inputDto.setModality("PRESENCIAL");
        inputDto.setProfessionalId(10L);
        inputDto.setRecurrenceCount(4); // 4 weekly sessions

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any(), eq("PSICOLOGIA"), eq(10L)))
                .thenReturn(List.of());

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        AppointmentDto result = appointmentService.create(inputDto);

        assertNotNull(result);
        assertNotNull(result.getRecurrenceGroupId());
        assertEquals("WEEKLY;COUNT=4", result.getRecurrenceRule());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(4)).save(captor.capture());

        List<Appointment> savedAppointments = captor.getAllValues();
        assertEquals(4, savedAppointments.size());

        String groupId = savedAppointments.get(0).getRecurrenceGroupId();
        for (int i = 0; i < 4; i++) {
            Appointment a = savedAppointments.get(i);
            assertEquals(groupId, a.getRecurrenceGroupId());
            assertEquals("WEEKLY;COUNT=4", a.getRecurrenceRule());
            assertEquals(LocalDate.of(2026, 9, 1).plusWeeks(i), a.getAppointmentDate());
            assertEquals(LocalTime.of(10, 0), a.getStartTime());
            assertEquals(LocalTime.of(11, 0), a.getEndTime());
        }
    }

    @Test
    void createRecurringAppointmentFailsAtomicallyIfOneWeekHasConflict() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        AppointmentDto inputDto = new AppointmentDto();
        inputDto.setPatientId(100L);
        inputDto.setAppointmentDate(LocalDate.of(2026, 9, 1));
        inputDto.setStartTime(LocalTime.of(10, 0));
        inputDto.setEndTime(LocalTime.of(11, 0));
        inputDto.setProfessionalId(10L);
        inputDto.setRecurrenceCount(3);

        LocalDate week3 = LocalDate.of(2026, 9, 1).plusWeeks(2); // 2026-09-15

        Appointment existingConflict = new Appointment();
        existingConflict.setId(999L);

        when(appointmentRepository.findOverlappingAppointments(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), LocalTime.of(11, 0), "PSICOLOGIA", 10L))
                .thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointments(LocalDate.of(2026, 9, 8), LocalTime.of(10, 0), LocalTime.of(11, 0), "PSICOLOGIA", 10L))
                .thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointments(week3, LocalTime.of(10, 0), LocalTime.of(11, 0), "PSICOLOGIA", 10L))
                .thenReturn(List.of(existingConflict));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(inputDto));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateStatusWithCancelSeriesCancelsAllFutureInRecurrenceGroup() {
        Appointment app1 = new Appointment();
        app1.setId(10L);
        app1.setPatient(patient);
        app1.setSpecialty("PSICOLOGIA");
        app1.setStatus("PROGRAMADA");
        app1.setAppointmentDate(LocalDate.of(2026, 9, 8));
        app1.setRecurrenceGroupId("rec-123");

        Appointment app2 = new Appointment();
        app2.setId(11L);
        app2.setPatient(patient);
        app2.setSpecialty("PSICOLOGIA");
        app2.setStatus("PROGRAMADA");
        app2.setAppointmentDate(LocalDate.of(2026, 9, 15));
        app2.setRecurrenceGroupId("rec-123");

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(app1));
        when(appointmentRepository.findByRecurrenceGroupIdAndSpecialtyAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                "rec-123", "PSICOLOGIA", LocalDate.of(2026, 9, 8)))
                .thenReturn(List.of(app1, app2));

        appointmentService.updateStatus(10L, "CANCELADA", true);

        assertEquals("CANCELADA", app1.getStatus());
        assertEquals("CANCELADA", app2.getStatus());
        verify(appointmentRepository).save(app1);
        verify(appointmentRepository).save(app2);
    }
}
