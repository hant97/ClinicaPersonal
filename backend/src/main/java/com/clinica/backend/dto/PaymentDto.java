package com.clinica.backend.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
@Data
public class PaymentDto {
    private Long id;
    @NotNull(message = "El paciente es obligatorio") private Long patientId;
    @NotNull(message = "El monto es obligatorio") @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero") private BigDecimal amount;
    @NotNull(message = "La fecha de pago es obligatoria") private LocalDateTime paymentDate;
    @Size(max = 50, message = "El método de pago es demasiado largo") private String paymentMethod; // Código de catálogo PAYMENT_METHOD
    private String description;
    private Long appointmentId;
    
    @Valid private List<PaymentItemDto> items;
    private String specialty;
    private String patientName;
}
