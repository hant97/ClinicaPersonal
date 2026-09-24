package com.clinica.backend.service;

import com.clinica.backend.dto.AllergyDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.mapper.AllergyMapperImpl;
import com.clinica.backend.model.Allergy;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.AllergyRepository;
import com.clinica.backend.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllergyServiceTest {

    @Mock
    private AllergyRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    private AllergyService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = user(10L, "PSICOLOGIA", "ROLE_STAFF");
        service = new AllergyService(repository, patientRepository, clinicalAuthorizationService, new AllergyMapperImpl());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAllergyUsesAuthenticatedProfessionalInsteadOfClientValue() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentProfessional()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.save(any(Allergy.class))).thenAnswer(invocation -> {
            Allergy allergy = invocation.getArgument(0);
            allergy.setId(11L);
            return allergy;
        });

        AllergyDto dto = new AllergyDto();
        dto.setAllergen("Penicilina");
        AllergyDto result = service.createAllergy(7L, dto);

        assertEquals(11L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("PSICOLOGIA", result.getSpecialty());
        assertEquals("Penicilina", result.getAllergen());
    }

    @Test
    void deletedAllergyIsNotFound() {
        when(repository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteAllergy(4L));
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
