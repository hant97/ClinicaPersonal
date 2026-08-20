package com.clinica.backend.controller;

import com.clinica.backend.dto.AuditLogDto;
import com.clinica.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditLogController).build();
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs should pass filters to service")
    void getAuditLogs_ShouldPassFilters() throws Exception {
        Page<AuditLogDto> empty = new PageImpl<>(List.of(), Pageable.ofSize(10), 0);

        when(auditLogService.getAuditLogs(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(empty);

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-20")
                        .param("username", "admin_test")
                        .param("specialty", "PSICOLOGIA")
                        .param("action", "CREATE")
                        .param("entityType", "PATIENT")
                        .param("query", "paciente")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> specialtyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> entityTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(auditLogService).getAuditLogs(
                startCaptor.capture(),
                endCaptor.capture(),
                userCaptor.capture(),
                specialtyCaptor.capture(),
                actionCaptor.capture(),
                entityTypeCaptor.capture(),
                queryCaptor.capture(),
                pageableCaptor.capture()
        );

        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(userCaptor.getValue()).isEqualTo("admin_test");
        assertThat(specialtyCaptor.getValue()).isEqualTo("PSICOLOGIA");
        assertThat(actionCaptor.getValue()).isEqualTo("CREATE");
        assertThat(entityTypeCaptor.getValue()).isEqualTo("PATIENT");
        assertThat(queryCaptor.getValue()).isEqualTo("paciente");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs/actions should return actions list")
    void getDistinctActions_ShouldReturnList() throws Exception {
        when(auditLogService.getDistinctActions()).thenReturn(List.of("CREATE", "DELETE", "LOGIN", "UPDATE"));

        mockMvc.perform(get("/api/v1/audit-logs/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("CREATE"))
                .andExpect(jsonPath("$[1]").value("DELETE"));

        verify(auditLogService, times(1)).getDistinctActions();
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs/entity-types should return entity types list")
    void getDistinctEntityTypes_ShouldReturnList() throws Exception {
        when(auditLogService.getDistinctEntityTypes()).thenReturn(List.of("AUTH", "PATIENT", "PAYMENT", "USER"));

        mockMvc.perform(get("/api/v1/audit-logs/entity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("AUTH"))
                .andExpect(jsonPath("$[1]").value("PATIENT"));

        verify(auditLogService, times(1)).getDistinctEntityTypes();
    }
}
