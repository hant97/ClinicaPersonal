package com.clinica.backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchTextTest {

    @Test
    void removesAccentsAndCase() {
        assertEquals("jose nunez", SearchText.normalize("  José NÚÑEZ "));
        assertEquals("muller", SearchText.normalize("Müller"));
        assertEquals("", SearchText.normalize(null));
    }

    @Test
    void joinsNonBlankParts() {
        assertEquals("ana perez 123", SearchText.of("Ana", " Pérez ", "123"));
        assertEquals("ana perez", SearchText.of("Ana", "Pérez", null));
    }

    @Test
    void patientKeepsSearchTextInSyncOnSave() {
        Patient patient = new Patient();
        patient.setFirstName("María José");
        patient.setLastName("Ñahui");
        patient.setIdentificationDocument("DNI-44");

        patient.prePersist();

        assertEquals("maria jose nahui dni-44", patient.getSearchText());
    }
}
