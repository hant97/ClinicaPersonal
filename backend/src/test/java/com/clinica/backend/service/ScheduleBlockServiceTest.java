package com.clinica.backend.service;

import com.clinica.backend.dto.ScheduleBlockDto;
import com.clinica.backend.model.ScheduleBlock;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ScheduleBlockRepository;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.mapper.ScheduleBlockMapperImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
class ScheduleBlockServiceTest {

    @Mock
    private ScheduleBlockRepository blockRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ScheduleBlockMapperImpl scheduleBlockMapper = new ScheduleBlockMapperImpl();
    @InjectMocks
    private ScheduleBlockService blockService;

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
    void createBlockSavesSuccessfully() {
        ScheduleBlockDto dto = new ScheduleBlockDto(
                null, 1L, null, null, "Vacaciones",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15),
                null, null, "Vacaciones anuales"
        );

        when(blockRepository.save(any(ScheduleBlock.class))).thenAnswer(invocation -> {
            ScheduleBlock b = invocation.getArgument(0);
            b.setId(5L);
            return b;
        });

        ScheduleBlockDto result = blockService.createBlock(dto);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Vacaciones", result.getTitle());
        assertEquals("PSICOLOGIA", result.getSpecialty());
    }

    @Test
    void createBlockThrowsWhenStartDateAfterEndDate() {
        ScheduleBlockDto dto = new ScheduleBlockDto(
                null, 1L, null, null, "Vacaciones",
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 1),
                null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> blockService.createBlock(dto));
        verify(blockRepository, never()).save(any());
    }

    @Test
    void validateNoBlockConflictDetectsAllDayBlock() {
        ScheduleBlock block = new ScheduleBlock();
        block.setId(1L);
        block.setTitle("Congreso Médico");
        block.setStartDate(LocalDate.of(2026, 8, 25));
        block.setEndDate(LocalDate.of(2026, 8, 25));
        block.setStartTime(null);
        block.setEndTime(null);

        when(blockRepository.findBlocksForDate("PSICOLOGIA", 1L, LocalDate.of(2026, 8, 25)))
                .thenReturn(List.of(block));

        assertThrows(IllegalArgumentException.class, () -> 
            blockService.validateNoBlockConflict(LocalDate.of(2026, 8, 25), LocalTime.of(10, 0), LocalTime.of(11, 0), 1L, "PSICOLOGIA")
        );
    }

    @Test
    void validateNoBlockConflictDetectsPartialTimeBlock() {
        ScheduleBlock block = new ScheduleBlock();
        block.setId(2L);
        block.setTitle("Reunión de equipo");
        block.setStartDate(LocalDate.of(2026, 8, 25));
        block.setEndDate(LocalDate.of(2026, 8, 25));
        block.setStartTime(LocalTime.of(14, 0));
        block.setEndTime(LocalTime.of(16, 0));

        when(blockRepository.findBlocksForDate("PSICOLOGIA", 1L, LocalDate.of(2026, 8, 25)))
                .thenReturn(List.of(block));

        // Overlapping: 15:00 - 15:30 -> conflict!
        assertThrows(IllegalArgumentException.class, () -> 
            blockService.validateNoBlockConflict(LocalDate.of(2026, 8, 25), LocalTime.of(15, 0), LocalTime.of(15, 30), 1L, "PSICOLOGIA")
        );

        // Non-overlapping: 10:00 - 11:00 -> no exception
        assertDoesNotThrow(() -> 
            blockService.validateNoBlockConflict(LocalDate.of(2026, 8, 25), LocalTime.of(10, 0), LocalTime.of(11, 0), 1L, "PSICOLOGIA")
        );
    }

    @Test
    void deleteBlockDeletesSuccessfully() {
        ScheduleBlock block = new ScheduleBlock();
        block.setId(7L);
        block.setSpecialty("PSICOLOGIA");

        when(blockRepository.findByIdAndSpecialty(7L, "PSICOLOGIA")).thenReturn(Optional.of(block));

        blockService.deleteBlock(7L);

        verify(blockRepository).delete(block);
    }

    @Test
    void listLoadsProfessionalNamesInOneBatch() {
        ScheduleBlock first = new ScheduleBlock();
        first.setId(1L);
        first.setProfessionalId(10L);
        first.setTitle("Capacitación");
        ScheduleBlock second = new ScheduleBlock();
        second.setId(2L);
        second.setProfessionalId(11L);
        second.setTitle("Vacaciones");
        User firstProfessional = new User();
        firstProfessional.setId(10L);
        firstProfessional.setFirstName("Ana");
        firstProfessional.setLastName("Rojas");
        User secondProfessional = new User();
        secondProfessional.setId(11L);
        secondProfessional.setFirstName("Luis");
        secondProfessional.setLastName("Vega");

        when(blockRepository.findBlocksInRange(eq("PSICOLOGIA"), isNull(), any(), any()))
                .thenReturn(List.of(first, second));
        when(userRepository.findByIdIn(Set.of(10L, 11L)))
                .thenReturn(List.of(firstProfessional, secondProfessional));

        List<ScheduleBlockDto> result = blockService.getBlocks(null, null, null);

        assertEquals(List.of("Ana Rojas", "Luis Vega"),
                result.stream().map(ScheduleBlockDto::getProfessionalName).toList());
        verify(userRepository, times(1)).findByIdIn(Set.of(10L, 11L));
        verify(userRepository, never()).findById(anyLong());
    }
}
