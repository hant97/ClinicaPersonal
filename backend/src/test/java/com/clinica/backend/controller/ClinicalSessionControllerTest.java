package com.clinica.backend.controller;

import com.clinica.backend.config.GlobalExceptionHandler;
import com.clinica.backend.dto.ClinicalSessionDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.service.ClinicalSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClinicalSessionControllerTest {

    @Mock
    private ClinicalSessionService clinicalSessionService;
    @InjectMocks
    private ClinicalSessionController clinicalSessionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clinicalSessionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSuccessForAuthorizedClinicalSession() throws Exception {
        ClinicalSessionDto session = new ClinicalSessionDto();
        session.setId(4L);
        when(clinicalSessionService.getSessionById(4L)).thenReturn(session);

        mockMvc.perform(get("/api/v1/clinical-sessions/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void returnsForbiddenWithoutClinicalResourceDetails() throws Exception {
        when(clinicalSessionService.getSessionById(4L))
                .thenThrow(new AccessDeniedException("Acceso fuera del ámbito autorizado"));

        mockMvc.perform(get("/api/v1/clinical-sessions/4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permisos suficientes para realizar esta acción"));
    }

    @Test
    void returnsNotFoundForDeletedOrUnknownClinicalSession() throws Exception {
        when(clinicalSessionService.getSessionById(4L))
                .thenThrow(new ResourceNotFoundException("Sesión clínica no encontrada"));

        mockMvc.perform(get("/api/v1/clinical-sessions/4"))
                .andExpect(status().isNotFound());
    }
}
