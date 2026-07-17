package com.clinica.backend.dto;

import com.clinica.backend.model.TransactionReason;
import com.clinica.backend.model.TransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDto {
    private Long id;
    private Long supplyId;
    private String supplyName; // Useful for UI
    private Integer quantity;
    private TransactionType type;
    private TransactionReason reason;
    private String referenceId;
    private String notes;
    private LocalDateTime transactionDate;
}
