package com.clinica.backend.service;

import com.clinica.backend.dto.SupplyStatsDto;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.SupplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplyServiceTest {

    private SupplyRepository supplyRepository;
    private WebsiteFileStorage websiteFileStorage;
    private SupplyService supplyService;

    @BeforeEach
    void setUp() {
        supplyRepository = mock(SupplyRepository.class);
        websiteFileStorage = mock(WebsiteFileStorage.class);
        supplyService = new SupplyService(supplyRepository, websiteFileStorage);

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
        when(supplyRepository.countBySpecialtyAndDeletedFalse("PSICOLOGIA")).thenReturn(20L);
        when(supplyRepository.countLowStockBySpecialty("PSICOLOGIA")).thenReturn(5L);
        when(supplyRepository.countOutOfStockBySpecialty("PSICOLOGIA")).thenReturn(2L);
        when(supplyRepository.countExpiringBetweenBySpecialty(any(), any(), eq("PSICOLOGIA"))).thenReturn(3L);
        when(supplyRepository.sumInventoryValueBySpecialty("PSICOLOGIA")).thenReturn(new BigDecimal("1234.50"));

        SupplyStatsDto stats = supplyService.getStats();

        assertEquals(20L, stats.getTotalSupplies());
        assertEquals(5L, stats.getLowStockCount());
        assertEquals(2L, stats.getOutOfStockCount());
        assertEquals(3L, stats.getExpiringSoonCount());
        assertEquals(new BigDecimal("1234.50"), stats.getInventoryValue());
    }
}
