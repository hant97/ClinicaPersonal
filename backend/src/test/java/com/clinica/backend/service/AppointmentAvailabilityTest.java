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
class AppointmentAvailabilityTest {

    @Mock
    private AppointmentRepository appointmentRepository;
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
    void createAppointmentFailsWhenOutsideProfessionalSchedule() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        AppointmentDto inputDto = new AppointmentDto();
        inputDto.setPatientId(100L);
        inputDto.setAppointmentDate(LocalDate.of(2026, 9, 1));
        inputDto.setStartTime(LocalTime.of(20, 0));
        inputDto.setEndTime(LocalTime.of(21, 0));
        inputDto.setProfessionalId(10L);

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        doThrow(new IllegalArgumentException("Fuera de jornada laboral"))
                .when(professionalScheduleService)
                .validateProfessionalAvailability(10L, LocalDate.of(2026, 9, 1), LocalTime.of(20, 0), LocalTime.of(21, 0), "PSICOLOGIA");

        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(inputDto));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointmentFailsWhenDateOverlapsScheduleBlock() {
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(100L, "PSICOLOGIA"))
                .thenReturn(Optional.of(patient));

        AppointmentDto inputDto = new AppointmentDto();
        inputDto.setPatientId(100L);
        inputDto.setAppointmentDate(LocalDate.of(2026, 9, 1));
        inputDto.setStartTime(LocalTime.of(10, 0));
        inputDto.setEndTime(LocalTime.of(11, 0));
        inputDto.setProfessionalId(10L);

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        doThrow(new IllegalArgumentException("Coincide con un bloqueo de agenda"))
                .when(scheduleBlockService)
                .validateNoBlockConflict(LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), LocalTime.of(11, 0), 10L, "PSICOLOGIA");

        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(inputDto));
        verify(appointmentRepository, never()).save(any());
    }
}
