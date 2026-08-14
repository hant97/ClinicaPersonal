package com.clinica.backend.integration;

import com.clinica.backend.config.CatalogDataInitializer;
import com.clinica.backend.config.DataSeeder;
import com.clinica.backend.config.MockDataSeeder;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.DermatologicalEvaluation;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.DermatologicalEvaluationRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiContractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ClinicalSessionRepository clinicalSessionRepository;

    @Autowired
    private DermatologicalEvaluationRepository dermatologicalEvaluationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testProfileDoesNotLoadApplicationOrMockSeeders() {
        assertTrue(applicationContext.getBeansOfType(DataSeeder.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(CatalogDataInitializer.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(MockDataSeeder.class).isEmpty());
        assertTrue(patientRepository.findAll().isEmpty());
    }

    @Test
    void authenticatesValidCredentialsAndRejectsInvalidCredentials() throws Exception {
        createUser("integration.login", "correct-password", "PSICOLOGIA", "ROLE_STAFF");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"integration.login","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.specialty").value("PSICOLOGIA"))
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"integration.login","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void protectsConfidentialClinicalSessionsWithTheAuthenticatedProfessional() throws Exception {
        User owner = createUser("session.owner", "password", "PSICOLOGIA", "ROLE_STAFF");
        User colleague = createUser("session.colleague", "password", "PSICOLOGIA", "ROLE_STAFF");
        Patient patient = createPatient();

        ClinicalSession session = new ClinicalSession();
        session.setPatient(patient);
        session.setSessionDate(LocalDate.of(2026, 8, 13));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(10, 0));
        session.setSessionType("Terapia individual");
        session.setModality("PRESENCIAL");
        session.setStatus("COMPLETADA");
        session.setConfidential(true);
        session.setSpecialty("PSICOLOGIA");
        session.setProfessionalId(owner.getId());
        session = clinicalSessionRepository.saveAndFlush(session);

        mockMvc.perform(get("/api/v1/clinical-sessions/{id}", session.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.confidential").value(true));

        mockMvc.perform(get("/api/v1/clinical-sessions/{id}", session.getId())
                        .header("Authorization", bearer(colleague)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("No tienes permisos suficientes para realizar esta acción"));
    }

    @Test
    void returnsZeroBasedDermatologyPagesWithStableMetadata() throws Exception {
        User dermatologist = createUser("dermatology.admin", "password", "DERMATOLOGIA", "ROLE_ADMIN");
        Patient patient = createPatient();
        saveEvaluation(patient, dermatologist, LocalDate.of(2026, 8, 10));
        saveEvaluation(patient, dermatologist, LocalDate.of(2026, 8, 11));
        saveEvaluation(patient, dermatologist, LocalDate.of(2026, 8, 12));

        mockMvc.perform(get("/api/v1/patients/{patientId}/dermatological-evaluations", patient.getId())
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", bearer(dermatologist)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].evaluationDate").value("2026-08-12"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/v1/patients/{patientId}/dermatological-evaluations", patient.getId())
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", bearer(dermatologist)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.number").value(1));
    }

    @Test
    void returnsStructuredValidationErrorsForInvalidPatientDto() throws Exception {
        User professional = createUser("validation.staff", "password", "PSICOLOGIA", "ROLE_STAFF");

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", bearer(professional))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"",
                                  "lastName":"",
                                  "dateOfBirth":"2999-01-01",
                                  "email":"correo-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación en los campos enviados"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void websiteEditorEndpointsRequireSiteAdminRole() throws Exception {
        User staff = createUser("editor.staff", "password", "PSICOLOGIA", "ROLE_STAFF");

        mockMvc.perform(get("/api/v1/admin/website/editor")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/website/publish").param("revision", "0")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
    }

    private User createUser(String username, String password, String specialty, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setSpecialty(specialty);
        user.setRoles(Set.of(role));
        return userRepository.saveAndFlush(user);
    }

    private Patient createPatient() {
        Patient patient = new Patient();
        patient.setFirstName("Paciente");
        patient.setLastName("Integración");
        patient.setIdentificationDocument("INTEGRATION-001");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender("Masculino");
        return patientRepository.saveAndFlush(patient);
    }

    private void saveEvaluation(Patient patient, User professional, LocalDate date) {
        DermatologicalEvaluation evaluation = new DermatologicalEvaluation();
        evaluation.setPatient(patient);
        evaluation.setProfessionalId(professional.getId());
        evaluation.setEvaluationDate(date);
        evaluation.setSkinType("Mixta");
        dermatologicalEvaluationRepository.saveAndFlush(evaluation);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
