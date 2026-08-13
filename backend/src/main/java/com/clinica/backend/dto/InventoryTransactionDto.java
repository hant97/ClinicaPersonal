package com.clinica.backend.dto;

import com.clinica.backend.model.TransactionReason;
import com.clinica.backend.model.TransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDto {
    private Long id;
    @NotNull(message = "El insumo es obligatorio") private Long supplyId;
    private String supplyName; // Useful for UI
    @NotNull(message = "La cantidad es obligatoria") @Positive(message = "La cantidad debe ser mayor que cero") private Integer quantity;
    @NotNull(message = "El tipo de movimiento es obligatorio") private TransactionType type;
    @NotNull(message = "El motivo de movimiento es obligatorio") private TransactionReason reason;
    private String referenceId;
    private String notes;
    private LocalDateTime transactionDate;
}
