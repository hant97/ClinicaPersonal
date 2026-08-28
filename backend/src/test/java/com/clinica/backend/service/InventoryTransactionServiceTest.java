package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.exception.ConflictException;
import com.clinica.backend.mapper.InventoryTransactionMapperImpl;
import com.clinica.backend.model.InventoryTransaction;
import com.clinica.backend.model.Supply;
import com.clinica.backend.model.TransactionReason;
import com.clinica.backend.model.TransactionType;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.InventoryTransactionRepository;
import com.clinica.backend.repository.SupplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryTransactionServiceTest {

    @Mock
    private InventoryTransactionRepository transactionRepository;
    @Mock
    private SupplyRepository supplyRepository;

    private InventoryTransactionService service;

    @BeforeEach
    void setUp() {
        service = new InventoryTransactionService(transactionRepository, supplyRepository, new InventoryTransactionMapperImpl());

        User user = new User();
        user.setSpecialty("PSICOLOGIA");
        user.setRoles(Set.of("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsOutputAfterLockingSupply() {
        Supply supply = supply(1L, 5);
        when(supplyRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(supply));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        InventoryTransactionDto result = service.recordTransaction(transaction(TransactionType.OUT, 3));

        assertEquals(2, supply.getCurrentStock());
        assertEquals(10L, result.getId());
        InOrder order = inOrder(supplyRepository, transactionRepository);
        order.verify(supplyRepository).findActiveByIdForUpdate(1L);
        order.verify(supplyRepository).save(supply);
        order.verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void insufficientStockLeavesNoPartialChanges() {
        Supply supply = supply(1L, 2);
        when(supplyRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(supply));

        assertThrows(ConflictException.class,
                () -> service.recordTransaction(transaction(TransactionType.OUT, 3)));

        assertEquals(2, supply.getCurrentStock());
        verify(supplyRepository, never()).save(any(Supply.class));
        verify(transactionRepository, never()).save(any(InventoryTransaction.class));
    }

    @Test
    void negativeAdjustmentLeavesNoPartialChanges() {
        Supply supply = supply(1L, 2);
        when(supplyRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(supply));

        assertThrows(ConflictException.class,
                () -> service.recordTransaction(transaction(TransactionType.ADJUSTMENT, -3)));

        assertEquals(2, supply.getCurrentStock());
        verify(supplyRepository, never()).save(any(Supply.class));
        verify(transactionRepository, never()).save(any(InventoryTransaction.class));
    }

    private Supply supply(Long id, int currentStock) {
        Supply supply = new Supply();
        supply.setId(id);
        supply.setName("Guantes");
        supply.setCurrentStock(currentStock);
        supply.setSpecialty("PSICOLOGIA");
        return supply;
    }

    private InventoryTransactionDto transaction(TransactionType type, int quantity) {
        InventoryTransactionDto dto = new InventoryTransactionDto();
        dto.setSupplyId(1L);
        dto.setType(type);
        dto.setQuantity(quantity);
        dto.setReason(TransactionReason.CLINICAL_USAGE);
        return dto;
    }
}
