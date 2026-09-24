package com.clinica.backend.integration;

import com.clinica.backend.config.CatalogDataInitializer;
import com.clinica.backend.config.DataSeeder;
import com.clinica.backend.config.MockDataSeeder;
import com.clinica.backend.model.ClinicalSession;
import com.clinica.backend.model.Catalog;
import com.clinica.backend.model.DermatologicalEvaluation;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalSessionRepository;
import com.clinica.backend.repository.CatalogRepository;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private CatalogRepository catalogRepository;

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
        createUser("integration.login", "correct-password", "PSICOLOGIA", "ROLE_PROFESIONAL");

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
        User owner = createUser("session.owner", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");
        User colleague = createUser("session.colleague", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");
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
        User dermatologist = createUser("dermatology.admin", "password", "DERMATOLOGIA", "ROLE_PROFESIONAL", "ROLE_ADMIN");
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
    void rejectsInvalidOrMassivePaginationParameters() throws Exception {
        User professional = createUser("pagination.staff", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");

        mockMvc.perform(get("/api/v1/patients")
                        .param("page", "-1")
                        .param("size", "20")
                        .header("Authorization", bearer(professional)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/patients")
                        .param("page", "0")
                        .param("size", "101")
                        .header("Authorization", bearer(professional)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogScopeComesFromAuthenticatedUser() throws Exception {
        User professional = createUser("catalog.professional", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");
        User clinicalAdmin = createUser("catalog.admin", "password", "DERMATOLOGIA", "ROLE_PROFESIONAL", "ROLE_ADMIN");
        User siteAdmin = createUser("catalog.site-admin", "password", "GENERAL", "ROLE_SITE_ADMIN");
        saveCatalog("CAT_GENERAL", "GENERAL");
        saveCatalog("CAT_PSYCHOLOGY", "PSICOLOGIA");
        saveCatalog("CAT_DERMATOLOGY", "DERMATOLOGIA");

        mockMvc.perform(get("/api/v1/catalogs")
                        .param("size", "100")
                        .header("Authorization", bearer(professional)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code")
                        .value(containsInAnyOrder("CAT_GENERAL", "CAT_PSYCHOLOGY")));

        mockMvc.perform(get("/api/v1/catalogs")
                        .param("specialty", "ALL")
                        .header("Authorization", bearer(professional)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/catalogs")
                        .param("specialty", "PSICOLOGIA")
                        .header("Authorization", bearer(clinicalAdmin)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/catalogs")
                        .param("specialty", "ALL")
                        .param("size", "100")
                        .header("Authorization", bearer(siteAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code")
                        .value(containsInAnyOrder("CAT_GENERAL", "CAT_PSYCHOLOGY", "CAT_DERMATOLOGY")));
    }

    @Test
    void returnsStructuredValidationErrorsForInvalidPatientDto() throws Exception {
        User professional = createUser("validation.staff", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");

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
        User staff = createUser("editor.staff", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");

        mockMvc.perform(get("/api/v1/admin/website/editor")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/website/publish").param("revision", "0")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pageableEndpointsKeepDefaultsAndRejectUnknownSortProperties() throws Exception {
        User professional = createUser("sort.professional", "password", "PSICOLOGIA", "ROLE_PROFESIONAL", "ROLE_ADMIN");
        createPatient();

        mockMvc.perform(get("/api/v1/patients").header("Authorization", bearer(professional)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10));

        mockMvc.perform(get("/api/v1/patients").param("sort", "lastName,asc").header("Authorization", bearer(professional)))
                .andExpect(status().isOk());

        // Consulta JPQL (@Query) y consulta derivada: ambas rechazan propiedades inexistentes con 400.
        mockMvc.perform(get("/api/v1/patients").param("sort", "noExiste").header("Authorization", bearer(professional)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/supplies").param("sort", "noExiste").header("Authorization", bearer(professional)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patientSearchIgnoresAccentsAndCase() throws Exception {
        User professional = createUser("search.professional", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");
        createPatient(); // "Paciente Integración"

        for (String query : new String[]{"integracion", "INTEGRACIÓN", "paciente integ"}) {
            mockMvc.perform(get("/api/v1/patients/search").param("query", query).header("Authorization", bearer(professional)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
        mockMvc.perform(get("/api/v1/patients/search").param("query", "otro").header("Authorization", bearer(professional)))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void onlyAdministratorsCanDeletePatientsAndPayments() throws Exception {
        User professional = createUser("delete.professional", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");
        User assistant = createUser("delete.assistant", "password", "PSICOLOGIA", "ROLE_ASISTENTE");
        User administrator = createUser("delete.admin", "password", "PSICOLOGIA", "ROLE_PROFESIONAL", "ROLE_ADMIN");
        Patient patient = createPatient();

        for (User nonAdmin : new User[]{professional, assistant}) {
            mockMvc.perform(delete("/api/v1/patients/{id}", patient.getId()).header("Authorization", bearer(nonAdmin)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/payments/{id}", 999).header("Authorization", bearer(nonAdmin)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/payments/{id}/transactions/{tx}", 999, 1).header("Authorization", bearer(nonAdmin)))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(delete("/api/v1/patients/{id}", patient.getId()).header("Authorization", bearer(administrator)))
                .andExpect(status().isNoContent());
    }

    @Test
    void clinicalRecordsRequireTheProfessionalRole() throws Exception {
        User assistant = createUser("clinical.assistant", "password", "PSICOLOGIA", "ROLE_ASISTENTE");
        User managerWithoutClinicalRole = createUser("clinical.manager", "password", "PSICOLOGIA", "ROLE_ADMIN");
        User professional = createUser("clinical.professional", "password", "PSICOLOGIA", "ROLE_PROFESIONAL");
        Patient patient = createPatient();

        for (User nonProfessional : new User[]{assistant, managerWithoutClinicalRole}) {
            // Recepción y gestión ven la ficha administrativa del paciente, no su historia clínica.
            mockMvc.perform(get("/api/v1/patients/{id}", patient.getId()).header("Authorization", bearer(nonProfessional)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/patients/{id}/allergies", patient.getId()).header("Authorization", bearer(nonProfessional)))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get("/api/v1/patients/{id}/allergies", patient.getId()).header("Authorization", bearer(professional)))
                .andExpect(status().isOk());
    }

    @Test
    void agendaProfessionalListExcludesUsersWithoutTheProfessionalRole() throws Exception {
        User professional = createUser("agenda.professional", "password", "PSICOLOGIA", "ROLE_PROFESIONAL", "ROLE_ADMIN");
        createUser("agenda.assistant", "password", "PSICOLOGIA", "ROLE_ASISTENTE");

        mockMvc.perform(get("/api/v1/users/professionals").header("Authorization", bearer(professional)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("agenda.professional")));
    }

    private User createUser(String username, String password, String specialty, String... roles) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setSpecialty(specialty);
        user.setRoles(Set.of(roles));
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

    private void saveCatalog(String code, String specialty) {
        Catalog catalog = new Catalog();
        catalog.setCode(code);
        catalog.setName(code);
        catalog.setSpecialty(specialty);
        catalogRepository.saveAndFlush(catalog);
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
