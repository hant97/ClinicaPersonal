package com.clinica.backend.service;

import com.clinica.backend.dto.DermatologicalHistoryDto;
import com.clinica.backend.model.DermatologicalHistory;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.DermatologicalHistoryRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.mapper.DermatologicalHistoryMapperImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DermatologicalHistoryServiceTest {

    @Mock
    private DermatologicalHistoryRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @Spy
    private DermatologicalHistoryMapperImpl dermatologicalHistoryMapper = new DermatologicalHistoryMapperImpl();
    @InjectMocks
    private DermatologicalHistoryService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = user(10L, "DERMATOLOGIA", "ROLE_STAFF");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void upsertHistoryCreatesWhenMissingUsingAuthenticatedProfessional() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "DERMATOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.findByPatientIdAndDeletedFalse(7L)).thenReturn(Optional.empty());
        when(repository.save(any(DermatologicalHistory.class))).thenAnswer(invocation -> {
            DermatologicalHistory history = invocation.getArgument(0);
            history.setId(1L);
            return history;
        });

        DermatologicalHistoryDto dto = new DermatologicalHistoryDto();
        dto.setSkinType("Seca");
        DermatologicalHistoryDto result = service.upsertHistory(7L, dto);

        assertEquals(1L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("Seca", result.getSkinType());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
