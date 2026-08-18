package com.clinica.backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentDtoSerializationTest {

    @Test
    void testAppointmentDtoSerializationFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        AppointmentDto dto = new AppointmentDto();
        dto.setId(1L);
        dto.setPaid(true);
        dto.setFirstTime(true);
        dto.setPaymentId(10L);
        dto.setPaymentAmount(BigDecimal.valueOf(100));
        dto.setStatus("PROGRAMADA");
        dto.setModality("PRESENCIAL");
        dto.setPatientId(1L);

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\"isPaid\":true"), "JSON should contain 'isPaid': true but was: " + json);
        assertTrue(json.contains("\"isFirstTime\":true"), "JSON should contain 'isFirstTime': true but was: " + json);
    }

    @Test
    void testDashboardAppointmentDtoSerializationFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        DashboardAppointmentDto dto = DashboardAppointmentDto.builder()
                .id(1L)
                .isPaid(true)
                .isFirstTime(true)
                .paymentId(10L)
                .paymentAmount(BigDecimal.valueOf(100))
                .status("PROGRAMADA")
                .modality("PRESENCIAL")
                .patientId(1L)
                .build();

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\"isPaid\":true"), "JSON should contain 'isPaid': true but was: " + json);
        assertTrue(json.contains("\"isFirstTime\":true"), "JSON should contain 'isFirstTime': true but was: " + json);
    }

    @Test
    void testClinicalSessionDtoSerializationFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ClinicalSessionDto dto = new ClinicalSessionDto();
        dto.setId(1L);
        dto.setConfidential(true);
        dto.setPatientId(1L);
        dto.setStatus("FINALIZADA");

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\"isConfidential\":true"), "JSON should contain 'isConfidential': true but was: " + json);
    }
}
