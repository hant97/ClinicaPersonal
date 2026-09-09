package com.clinica.backend.service;

import com.clinica.backend.dto.ClinicalServiceDto;
import com.clinica.backend.dto.ClinicalServiceStatsDto;
import com.clinica.backend.model.ClinicalService;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.ClinicalServiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
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
    private ClinicalServiceService clinicalServiceService;

    @BeforeEach
    void setUp() {
        clinicalServiceRepository = mock(ClinicalServiceRepository.class);
        clinicalServiceService = new ClinicalServiceService(clinicalServiceRepository, new com.clinica.backend.mapper.ClinicalServiceMapperImpl());

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
        dto.setCategory("TERAPIA");
        dto.setDurationMinutes(60);
        dto.setActive(null);

        ClinicalServiceDto created = clinicalServiceService.createService(dto);

        assertEquals(10L, created.getId());
        assertTrue(created.getActive());
        assertEquals("PSICOLOGIA", created.getSpecialty());
        assertEquals("TERAPIA", created.getCategory());
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
        dto.setCategory("EVALUACION");
        dto.setDurationMinutes(45);
        dto.setImageUrl("https://example.com/img.png");
        dto.setActive(false);

        ClinicalServiceDto updated = clinicalServiceService.updateService(10L, dto);

        assertEquals("Consulta Psicológica", updated.getName());
        assertEquals("EVALUACION", updated.getCategory());
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
        when(clinicalServiceRepository.findTopServicesByRevenueWithIdBySpecialty(any(), any(), eq("PSICOLOGIA"), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{2L, "Consulta General", 10L, new BigDecimal("500.00")},
                        new Object[]{1L, "Terapia de Pareja", 3L, new BigDecimal("240.00")}));
        when(clinicalServiceRepository.findTopServicesByQuantityWithIdBySpecialty(any(), any(), eq("PSICOLOGIA"), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{3L, "Sesión Grupal", 15L, new BigDecimal("300.00")},
                        new Object[]{2L, "Consulta General", 10L, new BigDecimal("500.00")}));

        ClinicalServiceStatsDto stats = clinicalServiceService.getStats();

        assertEquals(5L, stats.getTotalServices());
        assertEquals(4L, stats.getActiveCount());
        assertEquals(new BigDecimal("75.00"), stats.getAveragePrice());

        // Por ingresos: Consulta General (500) primero
        assertEquals("Consulta General", stats.getTopByRevenue().get(0).getName());

        // Por cantidad: Sesión Grupal (15) primero, independiente del ingreso
        assertEquals("Sesión Grupal", stats.getTopByQuantity().get(0).getName());
        assertEquals(15L, stats.getTopByQuantity().get(0).getQuantity());
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
