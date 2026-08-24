package com.clinica.backend.controller;

import com.clinica.backend.dto.PublicAppointmentConfirmationDto;
import com.clinica.backend.service.AppointmentConfirmationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PublicAppointmentControllerTest {

    private AppointmentConfirmationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AppointmentConfirmationService.class);
        mockMvc = standaloneSetup(new PublicAppointmentController(service)).build();
    }

    @Test
    void getDelegatesToReadOnlyOperation() throws Exception {
        PublicAppointmentConfirmationDto expected = PublicAppointmentConfirmationDto.builder()
                .confirmable(true)
                .message("Pendiente")
                .build();
        when(service.getConfirmationByToken("token")).thenReturn(expected);

        mockMvc.perform(get("/api/v1/public/appointments/confirm/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmable").value(true));

        verify(service).getConfirmationByToken("token");
        verifyNoMoreInteractions(service);
    }

    @Test
    void postDelegatesToConfirmationOperation() throws Exception {
        PublicAppointmentConfirmationDto expected = PublicAppointmentConfirmationDto.builder()
                .confirmed(true)
                .message("Confirmada")
                .build();
        when(service.confirmByToken("token")).thenReturn(expected);

        mockMvc.perform(post("/api/v1/public/appointments/confirm/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));

        verify(service).confirmByToken("token");
        verifyNoMoreInteractions(service);
    }
}
