package com.clinica.backend.controller;

import com.clinica.backend.dto.SpecialtyDto;
import com.clinica.backend.service.SpecialtyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SpecialtyControllerTest {

    @Mock
    private SpecialtyService specialtyService;

    @InjectMocks
    private SpecialtyController specialtyController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SpecialtyDto psychologyDto;
    private SpecialtyDto dermatologyDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(specialtyController).build();
        psychologyDto = new SpecialtyDto(1L, "PSICOLOGIA", "Psicología", "Salud mental", "Brain", true, 1, null, null);
        dermatologyDto = new SpecialtyDto(2L, "DERMATOLOGIA", "Dermatología", "Piel", "Stethoscope", true, 2, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/specialties should return active specialties")
    void getActiveSpecialties_ShouldReturnList() throws Exception {
        when(specialtyService.getActiveSpecialties()).thenReturn(List.of(psychologyDto, dermatologyDto));

        mockMvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("PSICOLOGIA"))
                .andExpect(jsonPath("$[1].code").value("DERMATOLOGIA"));

        verify(specialtyService, times(1)).getActiveSpecialties();
    }

    @Test
    @DisplayName("GET /api/v1/specialties/all should return all specialties")
    void getAllSpecialties_ShouldReturnList() throws Exception {
        when(specialtyService.getAllSpecialties()).thenReturn(List.of(psychologyDto, dermatologyDto));

        mockMvc.perform(get("/api/v1/specialties/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(specialtyService, times(1)).getAllSpecialties();
    }

    @Test
    @DisplayName("GET /api/v1/specialties/{code} should return single specialty")
    void getSpecialtyByCode_ShouldReturnDto() throws Exception {
        when(specialtyService.getSpecialtyByCode("PSICOLOGIA")).thenReturn(psychologyDto);

        mockMvc.perform(get("/api/v1/specialties/PSICOLOGIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PSICOLOGIA"))
                .andExpect(jsonPath("$.name").value("Psicología"));

        verify(specialtyService, times(1)).getSpecialtyByCode("PSICOLOGIA");
    }

    @Test
    @DisplayName("POST /api/v1/specialties should create new specialty")
    void createSpecialty_ShouldReturnCreated() throws Exception {
        SpecialtyDto newDto = new SpecialtyDto(null, "NUTRICION", "Nutrición", "Atención nutricional", "Apple", true, 3, null, null);
        SpecialtyDto createdDto = new SpecialtyDto(3L, "NUTRICION", "Nutrición", "Atención nutricional", "Apple", true, 3, null, null);

        when(specialtyService.createSpecialty(any(SpecialtyDto.class))).thenReturn(createdDto);

        mockMvc.perform(post("/api/v1/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.code").value("NUTRICION"));

        verify(specialtyService, times(1)).createSpecialty(any(SpecialtyDto.class));
    }

    @Test
    @DisplayName("PUT /api/v1/specialties/{id} should update specialty")
    void updateSpecialty_ShouldReturnUpdated() throws Exception {
        SpecialtyDto updateDto = new SpecialtyDto(1L, "PSICOLOGIA", "Psicología Clínica", "Actualizado", "Brain", true, 1, null, null);

        when(specialtyService.updateSpecialty(eq(1L), any(SpecialtyDto.class))).thenReturn(updateDto);

        mockMvc.perform(put("/api/v1/specialties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Psicología Clínica"));

        verify(specialtyService, times(1)).updateSpecialty(eq(1L), any(SpecialtyDto.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/specialties/{id}/toggle-active should toggle status")
    void toggleActive_ShouldReturnUpdatedStatus() throws Exception {
        SpecialtyDto toggledDto = new SpecialtyDto(1L, "PSICOLOGIA", "Psicología", "Salud mental", "Brain", false, 1, null, null);

        when(specialtyService.toggleActive(1L, false)).thenReturn(toggledDto);

        mockMvc.perform(patch("/api/v1/specialties/1/toggle-active").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(specialtyService, times(1)).toggleActive(1L, false);
    }
}
