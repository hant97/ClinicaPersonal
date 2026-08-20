package com.clinica.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class PaymentTransactionDto {
    private Long id;
    private Long paymentId;
    @NotNull(message = "El monto del abono es obligatorio") @DecimalMin(value = "0.01", message = "El monto del abono debe ser mayor que cero") private BigDecimal amount;
    private LocalDateTime transactionDate; // Opcional: por defecto la fecha actual
    @Size(max = 50, message = "El método de pago es demasiado largo") private String paymentMethod; // Código de catálogo PAYMENT_METHOD
    @Size(max = 255, message = "La nota es demasiado larga") private String notes;
}
