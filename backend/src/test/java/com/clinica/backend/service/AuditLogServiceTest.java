package com.clinica.backend.service;

import com.clinica.backend.dto.AuditLogDto;
import com.clinica.backend.model.AuditLog;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setUsername("admin_test");
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
    void shouldRecordAuditWithSecurityContext() {
        auditLogService.record("CREATE", "PATIENT", "123", "Creación de paciente Juan");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals("admin_test", saved.getUsername());
        assertEquals("PSICOLOGIA", saved.getSpecialty());
        assertEquals("CREATE", saved.getAction());
        assertEquals("PATIENT", saved.getEntityType());
        assertEquals("123", saved.getEntityId());
        assertEquals("Creación de paciente Juan", saved.getDetail());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void shouldRecordAuditExplicitlyWithoutSecurityContext() {
        SecurityContextHolder.clearContext();

        auditLogService.record(5L, "doctor_derm", "DERMATOLOGIA", "UPDATE", "PRESCRIPTION", "99", "Receta editada", "192.168.1.50");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals("doctor_derm", saved.getUsername());
        assertEquals("DERMATOLOGIA", saved.getSpecialty());
        assertEquals("UPDATE", saved.getAction());
        assertEquals("PRESCRIPTION", saved.getEntityType());
        assertEquals("99", saved.getEntityId());
        assertEquals("Receta editada", saved.getDetail());
        assertEquals("192.168.1.50", saved.getIp());
    }

    @Test
    void shouldNotThrowExceptionWhenRepositoryFails() {
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());

        assertDoesNotThrow(() -> auditLogService.record("DELETE", "PATIENT", "123", "Paciente eliminado"));
    }

    @Test
    void shouldGetAuditLogsWithFilters() {
        AuditLog entry = AuditLog.builder()
                .id(1L)
                .userId(10L)
                .username("admin_test")
                .specialty("PSICOLOGIA")
                .action("CREATE")
                .entityType("PATIENT")
                .entityId("50")
                .detail("Paciente nuevo")
                .ip("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(entry));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLogDto> result = auditLogService.getAuditLogs(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "admin",
                "PSICOLOGIA",
                "CREATE",
                "PATIENT",
                "nuevo",
                PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("CREATE", result.getContent().get(0).getAction());
        assertEquals("PATIENT", result.getContent().get(0).getEntityType());
        assertEquals("admin_test", result.getContent().get(0).getUsername());
    }

    @Test
    void shouldGetDistinctActionsAndEntityTypes() {
        when(auditLogRepository.findDistinctActions()).thenReturn(List.of("CREATE", "DELETE", "LOGIN", "UPDATE"));
        when(auditLogRepository.findDistinctEntityTypes()).thenReturn(List.of("AUTH", "PATIENT", "PAYMENT", "USER"));

        List<String> actions = auditLogService.getDistinctActions();
        List<String> entityTypes = auditLogService.getDistinctEntityTypes();

        assertEquals(4, actions.size());
        assertEquals(4, entityTypes.size());
        assertTrue(actions.contains("LOGIN"));
        assertTrue(entityTypes.contains("PATIENT"));
    }
}
