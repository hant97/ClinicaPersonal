package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.model.InventoryTransaction;
import com.clinica.backend.model.Supply;
import com.clinica.backend.model.TransactionType;
import com.clinica.backend.model.User;
import com.clinica.backend.repository.InventoryTransactionRepository;
import com.clinica.backend.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryTransactionRepository transactionRepository;
    private final SupplyRepository supplyRepository;

    @Transactional
    public InventoryTransactionDto recordTransaction(InventoryTransactionDto dto) {
        if (dto.getSupplyId() == null) {
            throw new IllegalArgumentException("El ID del insumo es obligatorio");
        }
        if (dto.getQuantity() == null || dto.getQuantity() == 0) {
            throw new IllegalArgumentException("La cantidad debe ser distinta de cero");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("El tipo de transacción es obligatorio");
        }

        Supply supply = supplyRepository.findByIdAndDeletedFalse(dto.getSupplyId())
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado o dado de baja"));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getSpecialty().equals(supply.getSpecialty())) {
            throw new AccessDeniedException("Insumo fuera de la especialidad del usuario");
        }

        int currentStock = supply.getCurrentStock() != null ? supply.getCurrentStock() : 0;
        int newStock;
        int recordedQuantity;

        if (dto.getType() == TransactionType.IN) {
            recordedQuantity = Math.abs(dto.getQuantity());
            newStock = currentStock + recordedQuantity;
        } else if (dto.getType() == TransactionType.OUT) {
            recordedQuantity = Math.abs(dto.getQuantity());
            if (currentStock < recordedQuantity) {
                throw new IllegalArgumentException("Stock insuficiente para el insumo '" + supply.getName() + "'. Stock actual: " + currentStock + ", requerido: " + recordedQuantity);
            }
            newStock = currentStock - recordedQuantity;
        } else { // ADJUSTMENT
            recordedQuantity = dto.getQuantity();
            newStock = currentStock + recordedQuantity;
            if (newStock < 0) {
                throw new IllegalArgumentException("El ajuste resultaría en un stock negativo (" + newStock + ") para el insumo: " + supply.getName());
            }
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setSupply(supply);
        transaction.setQuantity(recordedQuantity);
        transaction.setType(dto.getType());
        transaction.setReason(dto.getReason());
        transaction.setReferenceId(dto.getReferenceId());
        transaction.setNotes(dto.getNotes());

        supply.setCurrentStock(newStock);
        supplyRepository.save(supply);

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        return mapToDto(savedTransaction);
    }

    public List<InventoryTransactionDto> getTransactionsBySupply(Long supplyId) {
        Supply supply = supplyRepository.findByIdAndDeletedFalse(supplyId)
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado o dado de baja"));
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getSpecialty().equals(supply.getSpecialty())) {
            throw new AccessDeniedException("Insumo fuera de la especialidad del usuario");
        }
        return transactionRepository.findBySupplyIdOrderByTransactionDateDesc(supplyId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private InventoryTransactionDto mapToDto(InventoryTransaction transaction) {
        InventoryTransactionDto dto = new InventoryTransactionDto();
        dto.setId(transaction.getId());
        dto.setSupplyId(transaction.getSupply().getId());
        dto.setSupplyName(transaction.getSupply().getName());
        dto.setQuantity(transaction.getQuantity());
        dto.setType(transaction.getType());
        dto.setReason(transaction.getReason());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setNotes(transaction.getNotes());
        dto.setTransactionDate(transaction.getTransactionDate());
        return dto;
    }
}
