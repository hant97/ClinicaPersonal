package com.clinica.backend.service;

import com.clinica.backend.dto.AppointmentDto;
import com.clinica.backend.model.Appointment;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AppointmentRepository;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class AppointmentServiceTest {

    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private ClinicalServiceRepository clinicalServiceRepository;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        patientRepository = mock(PatientRepository.class);
        clinicalServiceRepository = mock(ClinicalServiceRepository.class);
        appointmentService = new AppointmentService(appointmentRepository, patientRepository, clinicalServiceRepository);

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

        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(dto));
        verify(appointmentRepository, never()).save(any());
    }
}
