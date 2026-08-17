package com.clinica.backend.service;

import com.clinica.backend.dto.PatientStatsDto;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.RiskAlertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientServiceTest {

    private PatientRepository patientRepository;
    private RiskAlertRepository riskAlertRepository;
    private WebsiteFileStorage websiteFileStorage;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        riskAlertRepository = mock(RiskAlertRepository.class);
        websiteFileStorage = mock(WebsiteFileStorage.class);
        patientService = new PatientService(patientRepository, riskAlertRepository, websiteFileStorage);

        User user = new User();
        user.setId(1L);
        user.setUsername("doctor");
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStatsShouldAggregateCounters() {
        when(patientRepository.countBySpecialtyAndDeletedFalse("PSICOLOGIA")).thenReturn(120L);
        when(patientRepository.countNewPatientsBetween(eq("PSICOLOGIA"), any(), any())).thenReturn(8L);
        when(riskAlertRepository.countDistinctPatientsWithActiveAlerts("PSICOLOGIA")).thenReturn(5L);
        when(patientRepository.countMinorsBySpecialty(eq("PSICOLOGIA"), any())).thenReturn(12L);

        PatientStatsDto stats = patientService.getStats();

        assertEquals(120L, stats.getTotalPatients());
        assertEquals(8L, stats.getNewThisMonth());
        assertEquals(5L, stats.getWithActiveAlerts());
        assertEquals(12L, stats.getMinors());
    }
}
