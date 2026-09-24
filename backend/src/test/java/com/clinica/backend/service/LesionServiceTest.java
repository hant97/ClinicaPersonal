package com.clinica.backend.service;

import com.clinica.backend.dto.LesionDto;
import com.clinica.backend.model.Lesion;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.LesionRepository;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.mapper.LesionMapperImpl;
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
class LesionServiceTest {

    @Mock
    private LesionRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @Spy
    private LesionMapperImpl lesionMapper = new LesionMapperImpl();
    @InjectMocks
    private LesionService service;

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
    void createLesionUsesAuthenticatedProfessionalInsteadOfClientValue() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "DERMATOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.save(any(Lesion.class))).thenAnswer(invocation -> {
            Lesion lesion = invocation.getArgument(0);
            lesion.setId(9L);
            return lesion;
        });

        LesionDto dto = new LesionDto();
        dto.setPatientId(7L);
        dto.setProfessionalId(999L);
        dto.setLesionType("Mácula");
        LesionDto result = service.createLesion(7L, dto);

        assertEquals(9L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("Mácula", result.getLesionType());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
