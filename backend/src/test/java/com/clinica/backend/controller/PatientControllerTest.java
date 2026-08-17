package com.clinica.backend.controller;

import com.clinica.backend.dto.PatientDto;
import com.clinica.backend.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PatientController(patientService)).build();
    }

    @Test
    void getAllPatientsPassesFiltersToService() throws Exception {
        Page<PatientDto> empty = new PageImpl<>(List.of(), Pageable.ofSize(10), 0);
        when(patientService.getAllPatients(any(), any(), any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/api/v1/patients")
                        .param("active", "true")
                        .param("gender", "FEMENINO")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> activeCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> genderCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientService).getAllPatients(activeCaptor.capture(), genderCaptor.capture(), pageableCaptor.capture());

        assertThat(activeCaptor.getValue()).isTrue();
        assertThat(genderCaptor.getValue()).isEqualTo("FEMENINO");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void uploadPhotoReturnsUpdatedPatient() throws Exception {
        PatientDto updated = new PatientDto();
        updated.setId(1L);
        updated.setFirstName("Ana");
        updated.setLastName("Pérez");
        when(patientService.uploadPhoto(eq(1L), any())).thenReturn(updated);

        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", "dummy".getBytes());

        mockMvc.perform(multipart("/api/v1/patients/1/photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }
}
