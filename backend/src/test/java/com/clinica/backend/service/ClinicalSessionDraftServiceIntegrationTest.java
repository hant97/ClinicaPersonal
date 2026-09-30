package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalSessionDraftRequest;
import com.clinica.backend.dto.ClinicalSessionDraftResponse;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalSessionDraftRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ClinicalSessionDraftServiceIntegrationTest {

    @Autowired
    private ClinicalSessionDraftService draftService;
    @Autowired
    private ClinicalSessionDraftRepository draftRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private Long patientId;

    @BeforeEach
    void setUp() {
        User professional = new User();
        professional.setUsername("draft.version.test");
        professional.setPassword("test-hash");
        professional.setSpecialty("PSICOLOGIA");
        professional.setRoles(Set.of("ROLE_PROFESIONAL"));
        professional = userRepository.save(professional);

        Patient patient = new Patient();
        patient.setFirstName("Paciente");
        patient.setLastName("Borrador");
        patient.setGender("OTRO");
        patient.setSpecialty("PSICOLOGIA");
        patientId = patientRepository.save(patient).getId();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(professional, null, professional.getAuthorities()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        draftRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void staleTabCannotOverwriteOrDeleteNewerDraft() {
        ClinicalSessionDraftResponse first = draftService.saveDraft(patientId, request("primera", null));
        ClinicalSessionDraftResponse newer = draftService.saveDraft(patientId, request("segunda", first.getVersion()));

        assertEquals(0L, first.getVersion());
        assertEquals(1L, newer.getVersion());
        assertThrows(ConflictException.class,
                () -> draftService.saveDraft(patientId, request("antigua", first.getVersion())));
        assertThrows(ConflictException.class,
                () -> draftService.deleteDraft(patientId, first.getVersion()));
        assertEquals("segunda", draftService.getDraft(patientId).orElseThrow().getContent().get("subjective").asText());

        draftService.deleteDraft(patientId, newer.getVersion());
        assertFalse(draftService.getDraft(patientId).isPresent());
    }

    @Test
    void secondCreateWithoutVersionCannotReplaceExistingDraft() {
        draftService.saveDraft(patientId, request("primera", null));

        assertThrows(ConflictException.class,
                () -> draftService.saveDraft(patientId, request("segunda", null)));
    }

    private ClinicalSessionDraftRequest request(String subjective, Long version) {
        ClinicalSessionDraftRequest request = new ClinicalSessionDraftRequest();
        request.setContent(objectMapper.readTree("{\"subjective\":\"" + subjective + "\"}"));
        request.setVersion(version);
        return request;
    }
}
