package com.clinica.backend.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class PaymentDto {
    private Long id;
    private Long patientId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMethod; // CASH, TRANSFER, CARD
    private String description;
    
    private java.util.List<PaymentItemDto> items;
}
