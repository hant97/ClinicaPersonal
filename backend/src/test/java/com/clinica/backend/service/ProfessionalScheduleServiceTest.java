package com.clinica.backend.service;

import com.clinica.backend.dto.ProfessionalScheduleDto;
import com.clinica.backend.dto.WeeklyScheduleDto;
import com.clinica.backend.model.ProfessionalSchedule;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ProfessionalScheduleRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalScheduleServiceTest {

    @Mock
    private ProfessionalScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfessionalScheduleService scheduleService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("psicologo1");
        currentUser.setFirstName("Carlos");
        currentUser.setLastName("Gómez");
        currentUser.setSpecialty("PSICOLOGIA");
        currentUser.setRoles(Set.of("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getWeeklyScheduleReturnsMappedDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        ProfessionalSchedule ps = new ProfessionalSchedule();
        ps.setId(10L);
        ps.setProfessionalId(1L);
        ps.setDayOfWeek(1); // Lunes
        ps.setStartTime(LocalTime.of(9, 0));
        ps.setEndTime(LocalTime.of(17, 0));
        ps.setActive(true);
        ps.setSpecialty("PSICOLOGIA");

        when(scheduleRepository.findByProfessionalIdAndSpecialtyOrderByDayOfWeekAscStartTimeAsc(1L, "PSICOLOGIA"))
                .thenReturn(List.of(ps));

        WeeklyScheduleDto result = scheduleService.getWeeklySchedule(1L);

        assertNotNull(result);
        assertEquals(1L, result.getProfessionalId());
        assertEquals("Carlos Gómez", result.getProfessionalName());
        assertEquals(1, result.getSchedules().size());
        assertEquals(1, result.getSchedules().get(0).getDayOfWeek());
    }

    @Test
    void saveWeeklyScheduleValidatesAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        ProfessionalScheduleDto dto1 = new ProfessionalScheduleDto(null, 1L, 1, LocalTime.of(8, 0), LocalTime.of(14, 0), true, "PSICOLOGIA");
        ProfessionalScheduleDto dto2 = new ProfessionalScheduleDto(null, 1L, 2, LocalTime.of(9, 0), LocalTime.of(18, 0), true, "PSICOLOGIA");

        WeeklyScheduleDto weeklyDto = new WeeklyScheduleDto(1L, "Carlos Gómez", "PSICOLOGIA", List.of(dto1, dto2));

        when(scheduleRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyScheduleDto result = scheduleService.saveWeeklySchedule(weeklyDto);

        assertNotNull(result);
        verify(scheduleRepository).deleteByProfessionalIdAndSpecialty(1L, "PSICOLOGIA");
        verify(scheduleRepository).saveAll(anyList());
    }

    @Test
    void saveWeeklyScheduleThrowsWhenStartTimeAfterEndTime() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        ProfessionalScheduleDto invalidDto = new ProfessionalScheduleDto(null, 1L, 1, LocalTime.of(18, 0), LocalTime.of(9, 0), true, "PSICOLOGIA");
        WeeklyScheduleDto weeklyDto = new WeeklyScheduleDto(1L, "Carlos Gómez", "PSICOLOGIA", List.of(invalidDto));

        assertThrows(IllegalArgumentException.class, () -> scheduleService.saveWeeklySchedule(weeklyDto));
        verify(scheduleRepository, never()).saveAll(any());
    }

    @Test
    void isProfessionalAvailableReturnsTrueWhenNoSchedulesConfigured() {
        when(scheduleRepository.existsByProfessionalIdAndSpecialtyAndIsActiveTrue(1L, "PSICOLOGIA")).thenReturn(false);

        boolean available = scheduleService.isProfessionalAvailable(1L, LocalDate.of(2026, 8, 24), LocalTime.of(10, 0), LocalTime.of(11, 0), "PSICOLOGIA");
        assertTrue(available);
    }

    @Test
    void isProfessionalAvailableChecksDayAndHours() {
        when(scheduleRepository.existsByProfessionalIdAndSpecialtyAndIsActiveTrue(1L, "PSICOLOGIA")).thenReturn(true);

        LocalDate monday = LocalDate.of(2026, 8, 24); // 2026-08-24 is Monday (dayOfWeek = 1)

        ProfessionalSchedule ps = new ProfessionalSchedule();
        ps.setStartTime(LocalTime.of(9, 0));
        ps.setEndTime(LocalTime.of(13, 0));
        ps.setActive(true);

        when(scheduleRepository.findByProfessionalIdAndSpecialtyAndDayOfWeekAndIsActiveTrue(1L, "PSICOLOGIA", 1))
                .thenReturn(List.of(ps));

        // Inside slot
        assertTrue(scheduleService.isProfessionalAvailable(1L, monday, LocalTime.of(9, 30), LocalTime.of(10, 30), "PSICOLOGIA"));

        // Outside slot (starts early)
        assertFalse(scheduleService.isProfessionalAvailable(1L, monday, LocalTime.of(8, 30), LocalTime.of(9, 30), "PSICOLOGIA"));

        // Outside slot (ends late)
        assertFalse(scheduleService.isProfessionalAvailable(1L, monday, LocalTime.of(12, 30), LocalTime.of(13, 30), "PSICOLOGIA"));
    }
}
