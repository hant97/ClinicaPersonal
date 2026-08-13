package com.clinica.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentItemDto {
    private Long id;
    private Long paymentId;
    @NotBlank(message = "La descripción del ítem es obligatoria") private String description;
    @NotNull(message = "La cantidad es obligatoria") @Positive(message = "La cantidad debe ser mayor que cero") private Integer quantity;
    @NotNull(message = "El precio unitario es obligatorio") @DecimalMin(value = "0.0", inclusive = true, message = "El precio unitario no puede ser negativo") private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Long supplyId; // Nullable if not a physical good
    private Long clinicalServiceId; // Nullable if not a service
}
