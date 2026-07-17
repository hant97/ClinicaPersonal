package com.clinica.backend.service;

import com.clinica.backend.dto.InventoryTransactionDto;
import com.clinica.backend.model.InventoryTransaction;
import com.clinica.backend.model.Supply;
import com.clinica.backend.repository.InventoryTransactionRepository;
import com.clinica.backend.repository.SupplyRepository;
import lombok.RequiredArgsConstructor;
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
        Supply supply = supplyRepository.findByIdAndDeletedFalse(dto.getSupplyId())
                .orElseThrow(() -> new RuntimeException("Supply not found"));

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setSupply(supply);
        transaction.setQuantity(dto.getQuantity());
        transaction.setType(dto.getType());
        transaction.setReason(dto.getReason());
        transaction.setReferenceId(dto.getReferenceId());
        transaction.setNotes(dto.getNotes());

        // Update supply stock
        int currentStock = supply.getCurrentStock() != null ? supply.getCurrentStock() : 0;
        int newStock = currentStock + dto.getQuantity();
        
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock for supply: " + supply.getName());
        }
        
        supply.setCurrentStock(newStock);
        supplyRepository.save(supply);

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);
        return mapToDto(savedTransaction);
    }

    public List<InventoryTransactionDto> getTransactionsBySupply(Long supplyId) {
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
