package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalDocumentDto;
import com.clinica.backend.model.ClinicalDocument;
import com.clinica.backend.model.Patient;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalDocumentRepository;
import com.clinica.backend.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalDocumentServiceTest {

    @Mock
    private ClinicalDocumentRepository repository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ClinicalAuthorizationService clinicalAuthorizationService;
    @Mock
    private ClinicalDocumentStorage fileStorage;
    @InjectMocks
    private ClinicalDocumentService service;

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
    void uploadDocumentUsesAuthenticatedProfessionalSpecialtyAndFileMetadata() {
        Patient patient = new Patient();
        patient.setId(7L);
        MockMultipartFile file = new MockMultipartFile("file", "consentimiento.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(patientRepository.findByIdAndSpecialtyAndDeletedFalse(7L, "PSICOLOGIA")).thenReturn(Optional.of(patient));
        when(fileStorage.store(any(), eq("documents"))).thenReturn("documents/abc.pdf");
        when(repository.save(any(ClinicalDocument.class))).thenAnswer(invocation -> {
            ClinicalDocument document = invocation.getArgument(0);
            document.setId(9L);
            return document;
        });

        ClinicalDocumentDto result = service.uploadDocument(7L, file, "consentimiento", null, null);

        assertEquals(9L, result.getId());
        assertEquals(10L, result.getProfessionalId());
        assertEquals("PSICOLOGIA", result.getSpecialty());
        assertEquals("CONSENTIMIENTO", result.getCategory());
        assertEquals("consentimiento", result.getName());
        assertEquals("application/pdf", result.getMimeType());
        assertEquals(3L, result.getSizeBytes());
    }

    @Test
    void deleteDocumentMarksDeletedAndRemovesFile() {
        ClinicalDocument document = new ClinicalDocument();
        document.setId(5L);
        document.setSpecialty("PSICOLOGIA");
        document.setProfessionalId(10L);
        document.setFileUrl("documents/abc.pdf");
        when(repository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(document));
        when(clinicalAuthorizationService.currentUser()).thenReturn(user);
        when(repository.save(any(ClinicalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteDocument(5L);

        assertTrue(document.isDeleted());
        assertNotNull(document.getDeletedAt());
        assertEquals(10L, document.getDeletedBy());
        verify(fileStorage).delete("documents/abc.pdf");
    }

    private User user(Long id, String specialty, String role) {
        User u = new User();
        u.setId(id);
        u.setSpecialty(specialty);
        u.setRoles(Set.of(role));
        return u;
    }
}
