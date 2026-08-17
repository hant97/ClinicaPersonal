package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.dto.ClinicalServiceStatsDto;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalServiceRepository;
import com.clinica.backend.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClinicalServiceServiceTest {

    private ClinicalServiceRepository clinicalServiceRepository;
    private PaymentRepository paymentRepository;
    private ClinicalServiceService clinicalServiceService;

    @BeforeEach
    void setUp() {
        clinicalServiceRepository = mock(ClinicalServiceRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        clinicalServiceService = new ClinicalServiceService(clinicalServiceRepository, paymentRepository);

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
    void createServiceShouldDefaultActiveToTrueAndSetSpecialty() {
        when(clinicalServiceRepository.save(any(ClinicalService.class))).thenAnswer(invocation -> {
            ClinicalService saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ClinicalServiceDto dto = new ClinicalServiceDto();
        dto.setName("Terapia de Pareja");
        dto.setPrice(new BigDecimal("80.00"));
        dto.setCategory("Terapia");
        dto.setDurationMinutes(60);
        dto.setActive(null);

        ClinicalServiceDto created = clinicalServiceService.createService(dto);

        assertEquals(10L, created.getId());
        assertTrue(created.getActive());
        assertEquals("PSICOLOGIA", created.getSpecialty());
        assertEquals("Terapia", created.getCategory());
        assertEquals(60, created.getDurationMinutes());
    }

    @Test
    void updateServiceShouldApplyNewFields() {
        ClinicalService existing = new ClinicalService();
        existing.setId(10L);
        existing.setSpecialty("PSICOLOGIA");
        existing.setName("Viejo");
        existing.setActive(true);

        when(clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(10L, "PSICOLOGIA"))
                .thenReturn(Optional.of(existing));
        when(clinicalServiceRepository.save(any(ClinicalService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalServiceDto dto = new ClinicalServiceDto();
        dto.setName("Consulta Psicológica");
        dto.setPrice(new BigDecimal("50.00"));
        dto.setCategory("Evaluación");
        dto.setDurationMinutes(45);
        dto.setImageUrl("https://example.com/img.png");
        dto.setActive(false);

        ClinicalServiceDto updated = clinicalServiceService.updateService(10L, dto);

        assertEquals("Consulta Psicológica", updated.getName());
        assertEquals("Evaluación", updated.getCategory());
        assertEquals(45, updated.getDurationMinutes());
        assertEquals("https://example.com/img.png", updated.getImageUrl());
        assertFalse(updated.getActive());
    }

    @Test
    void getStatsShouldBuildTopListsOrdered() {
        when(clinicalServiceRepository.countBySpecialtyAndDeletedFalse("PSICOLOGIA")).thenReturn(5L);
        when(clinicalServiceRepository.countActiveBySpecialty("PSICOLOGIA")).thenReturn(4L);
        when(clinicalServiceRepository.averagePriceBySpecialty("PSICOLOGIA"))
                .thenReturn(new BigDecimal("75.00"));
        when(paymentRepository.findTopServicesWithIdBySpecialty(any(), any(), eq("PSICOLOGIA")))
                .thenReturn(List.<Object[]>of(
                        new Object[]{2L, "Consulta General", 10L, new BigDecimal("500.00")},
                        new Object[]{1L, "Terapia de Pareja", 3L, new BigDecimal("240.00")}));

        ClinicalServiceStatsDto stats = clinicalServiceService.getStats();

        assertEquals(5L, stats.getTotalServices());
        assertEquals(4L, stats.getActiveCount());
        assertEquals(new BigDecimal("75.00"), stats.getAveragePrice());

        // Por ingresos: Consulta General (500) primero
        assertEquals("Consulta General", stats.getTopByRevenue().get(0).getName());

        // Por cantidad: Consulta General (10) primero
        assertEquals("Consulta General", stats.getTopByQuantity().get(0).getName());
        assertEquals(10L, stats.getTopByQuantity().get(0).getQuantity());
    }

    @Test
    void deleteServiceShouldBeLogical() {
        ClinicalService existing = new ClinicalService();
        existing.setId(10L);
        existing.setSpecialty("PSICOLOGIA");

        when(clinicalServiceRepository.findByIdAndSpecialtyAndDeletedFalse(10L, "PSICOLOGIA"))
                .thenReturn(Optional.of(existing));

        clinicalServiceService.deleteService(10L);

        assertTrue(existing.isDeleted());
        verify(clinicalServiceRepository).save(existing);
    }
}
