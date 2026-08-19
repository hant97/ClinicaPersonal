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
    void testAppointmentDtoDeserializationWithExtraFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        String json = "{"
                + "\"patientId\":1,"
                + "\"appointmentDate\":\"2026-08-19\","
                + "\"startTime\":\"15:00\","
                + "\"endTime\":\"15:30\","
                + "\"status\":\"PROGRAMADA\","
                + "\"modality\":\"PRESENCIAL\","
                + "\"isFirstTime\":false,"
                + "\"firstTime\":false,"
                + "\"paid\":false,"
                + "\"patientName\":\"Carlos Lopez\","
                + "\"notes\":\"Consulta\""
                + "}";

        AppointmentDto dto = mapper.readValue(json, AppointmentDto.class);
        org.junit.jupiter.api.Assertions.assertNotNull(dto);
        org.junit.jupiter.api.Assertions.assertEquals(1L, dto.getPatientId());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 8, 19), dto.getAppointmentDate());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(15, 0), dto.getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(15, 30), dto.getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals("PROGRAMADA", dto.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("PRESENCIAL", dto.getModality());
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
