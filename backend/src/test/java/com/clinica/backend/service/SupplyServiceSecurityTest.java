package com.clinica.backend.service;

import com.clinica.backend.dto.SupplyDto;
import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.model.Supply;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.SupplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SupplyServiceSecurityTest {

    private SupplyRepository supplyRepository;
    private WebsiteFileStorage websiteFileStorage;
    private InventoryTransactionService inventoryTransactionService;
    private SupplyService supplyService;

    @BeforeEach
    void setUp() {
        supplyRepository = mock(SupplyRepository.class);
        websiteFileStorage = mock(WebsiteFileStorage.class);
        inventoryTransactionService = mock(InventoryTransactionService.class);
        supplyService = new SupplyService(supplyRepository, websiteFileStorage, inventoryTransactionService);

        User user = new User();
        user.setId(1L);
        user.setUsername("psychologist");
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
    void getSupplyByIdShouldFailIfSupplyBelongsToAnotherSpecialty() {
        Long supplyId = 99L;
        when(supplyRepository.findByIdAndSpecialtyAndDeletedFalse(supplyId, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplyService.getSupplyById(supplyId));
        verify(supplyRepository).findByIdAndSpecialtyAndDeletedFalse(supplyId, "PSICOLOGIA");
    }

    @Test
    void getSupplyByIdShouldSucceedIfSupplyBelongsToUserSpecialty() {
        Long supplyId = 10L;
        Supply supply = new Supply();
        supply.setId(supplyId);
        supply.setName("Test Manual");
        supply.setSpecialty("PSICOLOGIA");

        when(supplyRepository.findByIdAndSpecialtyAndDeletedFalse(supplyId, "PSICOLOGIA"))
                .thenReturn(Optional.of(supply));

        SupplyDto result = supplyService.getSupplyById(supplyId);
        assertNotNull(result);
        verify(supplyRepository).findByIdAndSpecialtyAndDeletedFalse(supplyId, "PSICOLOGIA");
    }

    @Test
    void updateSupplyShouldFailIfSupplyBelongsToAnotherSpecialty() {
        Long supplyId = 99L;
        SupplyDto dto = new SupplyDto();
        dto.setName("New Name");

        when(supplyRepository.findByIdAndSpecialtyAndDeletedFalse(supplyId, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplyService.updateSupply(supplyId, dto));
        verify(supplyRepository, never()).save(any());
    }

    @Test
    void deleteSupplyShouldFailIfSupplyBelongsToAnotherSpecialty() {
        Long supplyId = 99L;

        when(supplyRepository.findByIdAndSpecialtyAndDeletedFalse(supplyId, "PSICOLOGIA"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplyService.deleteSupply(supplyId));
        verify(supplyRepository, never()).save(any());
    }
}
