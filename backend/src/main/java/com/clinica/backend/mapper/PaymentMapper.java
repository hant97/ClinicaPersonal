package com.clinica.backend.mapper;

import com.clinica.backend.dto.PaymentDto;
import com.clinica.backend.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapea los campos directos de {@link Payment}. {@code paidAmount},
 * {@code balanceAmount}, {@code items} y {@code transactions} se completan
 * aparte en {@code PaymentService} porque son valores calculados o listas
 * filtradas, no una copia directa de campos.
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.fullName", target = "patientName")
    @Mapping(source = "appointment.id", target = "appointmentId")
    @Mapping(source = "clinicalSession.id", target = "clinicalSessionId")
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "balanceAmount", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    PaymentDto toDto(Payment payment);
}
