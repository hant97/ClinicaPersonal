package com.clinica.backend.service;

import com.clinica.backend.dto.PrescriptionDto;
import com.clinica.backend.dto.PrescriptionItemDto;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.Prescription;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.PatientRepository;
import com.clinica.backend.repository.PrescriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @InjectMocks
    private PrescriptionService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = user(10L, "PSICOLOGIA", "ROLE_STAFF");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPrescriptionUsesAuthenticatedProfessionalSpecialtyAndItems() {
        Patient patient = new Patient();
        patient.setId(7L);
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(repository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription prescription = invocation.getArgument(0);
            prescription.setId(3L);
            return prescription;
        });

        PrescriptionItemDto itemDto = new PrescriptionItemDto();
        itemDto.setName("Fluoxetina");
        itemDto.setDose("20 mg");

        PrescriptionDto dto = new PrescriptionDto();
        dto.setItems(List.of(itemDto));
        PrescriptionDto result = service.createPrescription(7L, dto);

        assertEquals(3L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("PSICOLOGIA", result.getSpecialty());
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        assertEquals("Fluoxetina", result.getItems().get(0).getName());
        assertEquals("20 mg", result.getItems().get(0).getDose());
    }

    @Test
    void deletePrescriptionMarksDeletedWithCurrentUser() {
        Prescription prescription = new Prescription();
        prescription.setId(5L);
        prescription.setSpecialty("PSICOLOGIA");
        prescription.setProfessionalId(10L);
        when(repository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(prescription));
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(repository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deletePrescription(5L);

        assertTrue(prescription.isDeleted());
        assertNotNull(prescription.getDeletedAt());
        assertEquals(10L, prescription.getDeletedBy());
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
